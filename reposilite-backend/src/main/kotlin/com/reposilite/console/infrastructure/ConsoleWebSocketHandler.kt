/*
 * Copyright (c) 2021 dzikoysk
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
import com.reposilite.console.ConsoleFacade
import com.reposilite.token.api.AccessTokenPermission.MANAGER
import com.reposilite.web.ReposiliteContext
import com.reposilite.web.ReposiliteContextFactory
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import panda.std.Result
import panda.std.Result.error
import panda.std.Result.ok
import panda.utilities.StringUtils
import java.util.function.Consumer

private const val AUTHORIZATION_PREFIX = "Authorization:"

internal class CliEndpoint(
    private val contextFactory: ReposiliteContextFactory,
    private val authenticationFacade: AuthenticationFacade,
    private val consoleFacade: ConsoleFacade,
    private val reposiliteJournalist: ReposiliteJournalist
) : Consumer<WsConfig> {

    @OpenApi(
        path = "/api/console/sock",
        methods = [HttpMethod.PATCH]
    )
    override fun accept(ws: WsConfig) {
        ws.onConnect { connection ->
            ws.onMessage { messageContext ->
                val context = contextFactory.create(connection)

                authenticateContext(context, messageContext)
                    .peek {
                        context.logger.info("CLI | $it accessed remote console")
                        initializeAuthenticatedContext(ws, connection, context, it)
                    }
                    .onError {
                        connection.send(it)
                        connection.session.disconnect()
                    }
            }
        }
    }

    private fun authenticateContext(context: ReposiliteContext, connection: WsMessageContext): Result<String, String> {
        val authMessage = connection.message()

        if (!authMessage.startsWith(AUTHORIZATION_PREFIX)) {
            context.logger.info("CLI | Unauthorized CLI access request from ${context.address} (missing credentials)")
            return error("Unauthorized connection request")
        }

        val credentials = StringUtils.replaceFirst(authMessage, AUTHORIZATION_PREFIX, "")
        val auth = authenticationFacade.authenticateByCredentials(credentials)

        if (!auth.isOk || !auth.get().hasPermission(MANAGER)) {
            context.logger.info("CLI | Unauthorized CLI access request from " + context.address)
            return error("Unauthorized connection request")
        }

        return ok("${auth.get().name}@${context.address}")
    }

    private fun initializeAuthenticatedContext(ws: WsConfig, connection: WsContext, context: ReposiliteContext, session: String) {
        ws.onMessage {
            when(val message = it.message()) {
                "keep-alive" -> connection.send("keep-alive")
                else -> {
                    context.logger.info("CLI | $session> $message")
                    consoleFacade.executeCommand(message)
                }
            }
        }

        val subscriberId = reposiliteJournalist.subscribe {
            connection.send(it.value)
        }

        ws.onClose {
            context.logger.info("CLI | $session closed connection")
            reposiliteJournalist.unsubscribe(subscriberId)
        }

        for (message in reposiliteJournalist.cachedLogger.messages) {
            connection.send(message.value)
        }
    }

}