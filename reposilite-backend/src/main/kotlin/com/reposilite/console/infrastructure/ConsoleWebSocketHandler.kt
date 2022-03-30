/*
 * Copyright (c) 2022 dzikoysk
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
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.extractFromString
import com.reposilite.web.http.unauthorized
import com.reposilite.web.http.unauthorizedError
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import panda.std.Result
import panda.std.reactive.Reference
import panda.utilities.StringUtils
import java.util.function.Consumer

private const val AUTHORIZATION_PREFIX = "Authorization:"

internal class CliEndpoint(
    private val journalist: ReposiliteJournalist,
    private val accessTokenFacade: AccessTokenFacade,
    private val authenticationFacade: AuthenticationFacade,
    private val consoleFacade: ConsoleFacade,
    private val forwardedIp: Reference<String>
) : Consumer<WsConfig> {

    @OpenApi(
        path = "/api/console/sock",
        methods = [HttpMethod.PATCH],
        tags = ["Console"]
    )
    override fun accept(ws: WsConfig) {
        ws.onConnect { connection ->
            ws.onMessage { messageContext ->
                authenticateContext(messageContext)
                    .peek {
                        journalist.logger.info("CLI | $it accessed remote console")
                        initializeAuthenticatedContext(ws, connection, it)
                    }
                    .onError {
                        connection.send(it)
                        connection.session.disconnect()
                    }
            }
        }
    }

    private fun authenticateContext(connection: WsMessageContext): Result<String, ErrorResponse> {
        val authMessage = connection.message()

        if (!authMessage.startsWith(AUTHORIZATION_PREFIX)) {
            journalist.logger.info("CLI | Unauthorized CLI access request from ${address(connection)} (missing credentials)")
            return unauthorizedError("Unauthorized connection request")
        }

        return authenticationMessageToCredentials(authMessage)
            .map { (name, secret) -> Credentials(name, secret) }
            .flatMap { authenticationFacade.authenticateByCredentials(it) }
            .filter({ accessTokenFacade.hasPermission(it.identifier, MANAGER) }, {
                journalist.logger.info("CLI | Unauthorized CLI access request from ${address(connection)}")
                unauthorized("Unauthorized connection request")
            })
            .map { "${it.name}@${address(connection)}" }
    }

    private fun authenticationMessageToCredentials(message: String): Result<Credentials, ErrorResponse> =
        extractFromString(StringUtils.replaceFirst(message, AUTHORIZATION_PREFIX, ""))
            .map { (name, secret) -> Credentials(name, secret) }

    private fun initializeAuthenticatedContext(ws: WsConfig, connection: WsContext, session: String) {
        ws.onMessage {
            when(val message = it.message()) {
                "keep-alive" -> connection.send("keep-alive")
                else -> {
                    journalist.logger.info("CLI | $session> $message")
                    consoleFacade.executeCommand(message)
                }
            }
        }

        val subscriberId = journalist.subscribe {
            connection.send(it.value)
        }

        ws.onClose {
            journalist.logger.info("CLI | $session closed connection")
            journalist.unsubscribe(subscriberId)
        }

        for (message in journalist.cachedLogger.messages) {
            connection.send(message.value)
        }
    }

    private fun address(context: WsContext): String =
        context.header(forwardedIp.get()) ?: context.session.remoteAddress.toString()

}