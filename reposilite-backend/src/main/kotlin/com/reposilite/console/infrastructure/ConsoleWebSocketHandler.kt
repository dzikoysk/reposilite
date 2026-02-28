/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reposilite.console.infrastructure

import com.reposilite.ReposiliteJournalist
import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.api.Credentials
import com.reposilite.console.ConsoleFacade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.extractFromString
import com.reposilite.shared.unauthorized
import com.reposilite.shared.unauthorizedError
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenPermission.MANAGER
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import panda.std.Result
import panda.std.reactive.Reference
import java.nio.channels.ClosedChannelException
import java.util.WeakHashMap
import java.util.function.Consumer

private const val AUTHORIZATION_PREFIX = "Authorization:"

private data class WsSession(
    val identifier: String,
    val subscriberId: Int,
)

internal class ConsoleWebSocketHandler(
    private val journalist: ReposiliteJournalist,
    private val accessTokenFacade: AccessTokenFacade,
    private val authenticationFacade: AuthenticationFacade,
    private val consoleFacade: ConsoleFacade,
    private val forwardedIp: Reference<String>
) : Consumer<WsConfig> {

    private val users: WeakHashMap<WsContext, WsSession> = WeakHashMap()

    @OpenApi(
        path = "/api/console/sock",
        methods = [HttpMethod.PATCH],
        tags = ["Console"]
    )
    override fun accept(ws: WsConfig) {
        ws.onMessage { ctx ->
            when (val session = users[ctx]) {
                null ->
                    authenticateContext(ctx)
                        .peek { identifier ->
                            journalist.logger.info("CLI | $identifier accessed remote console")

                            val subscriberId = journalist.subscribe {
                                try {
                                    ctx.send(it.value)
                                } catch (ignored: ClosedChannelException) {
                                    journalist.logger.debug("CLI | $identifier tried to write to closed channel")
                                }
                            }

                            users[ctx] = WsSession(identifier, subscriberId)

                            journalist.cachedLogger.messages
                                .filter { it.key.priority >= journalist.visibleThreshold.priority }
                                .forEach { message -> ctx.send(message.value) }
                        }
                        .onError {
                            journalist.logger.info("CLI | ${it.message} (${it.status})")
                            ctx.send(it)
                            ctx.session.disconnect()
                        }
                else ->
                    when (val message = ctx.message()) {
                        "keep-alive" -> ctx.send("keep-alive")
                        else -> {
                            journalist.logger.info("CLI | ${session.identifier}> $message")
                            consoleFacade.executeCommand(message)
                        }
                    }
            }
        }
        ws.onClose { ctx ->
            val session = users.remove(ctx) ?: return@onClose
            journalist.logger.info("CLI | ${session.identifier} closed connection")
            journalist.unsubscribe(session.subscriberId)
        }
    }

    private fun authenticateContext(connection: WsMessageContext): Result<String, ErrorResponse> {
        val authMessage = connection.message()

        if (!authMessage.startsWith(AUTHORIZATION_PREFIX)) {
            return unauthorizedError("Unauthorized CLI access request from ${connection.getHost()} (missing credentials)")
        }

        return extractFromString(authMessage.replaceFirst(AUTHORIZATION_PREFIX, ""))
            .map { (name, secret) ->
                Credentials(
                    host = connection.getHost(),
                    name = name,
                    secret = secret
                )
            }
            .flatMap { authenticationFacade.authenticateByCredentials(it) }
            .filter(
                { accessTokenFacade.hasPermission(it.identifier, MANAGER) },
                { unauthorized("Unauthorized CLI access request from ${connection.getHost()}") }
            )
            .map { "${it.name}@${connection.getHost()}" }
    }

    private fun WsContext.getHost(): String =
        header(forwardedIp.get()) ?: session.remoteAddress.toString()

}
