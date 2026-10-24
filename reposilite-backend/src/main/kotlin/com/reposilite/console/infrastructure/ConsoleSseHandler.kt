/*
 * Copyright (c) 2020-2026 dzikoysk
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
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.extractFromHeader
import com.reposilite.shared.unauthorized
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenPermission
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.sse.SseClient
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import panda.std.Result
import panda.std.reactive.Reference
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private const val SSE_EVENT_NAME = "log"

private class SseSession(
    val identifier: String,
    val subscriberId: Int,
    val scheduler: ScheduledFuture<*>,
    val completion: CompletableFuture<Void>
) {
    var closing = false
}

internal class ConsoleSseHandler(
    private val journalist: ReposiliteJournalist,
    private val accessTokenFacade: AccessTokenFacade,
    private val authenticationFacade: AuthenticationFacade,
    private val forwardedIp: Reference<String>,
    private val scheduler: ScheduledExecutorService
) {

    private val sessions = ConcurrentHashMap<SseClient, SseSession>()

    @Volatile
    private var stopping = false

    fun close() {
        val clients = synchronized(this) {
            stopping = true
            sessions.toMap()
        }
        clients.values.forEach(::unsubscribe)
        clients.forEach(::closeSession)
    }

    private fun unsubscribe(session: SseSession) {
        session.scheduler.cancel(false)
        journalist.unsubscribe(session.subscriberId)
    }

    private fun closeSession(sse: SseClient, session: SseSession) {
        synchronized(session) {
            if (session.closing) {
                return
            }
            session.closing = true
            unsubscribe(session)
            sse.close()
            try {
                session.completion.complete(null)
            } finally {
                sessions.remove(sse, session)
            }
        }
        if (!stopping) {
            journalist.logger.info("CLI | ${session.identifier} closed connection")
        }
    }

    @OpenApi(
        path = "/api/console/log",
        methods = [HttpMethod.GET],
        headers = [OpenApiParam(name = "Authorization", description = "Name and secret provided as basic auth credentials", required = true)],
        description = "Streams the output of logs through an SSE Connection.",
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Continuously sends out the log as messages under the `log` event. Sends a keepalive ping through comments."
            )
        ],
        tags = ["Console"]
    )
    fun handleSseLiveLog(sse: SseClient) {
        authenticateContext(sse.ctx())
            .peek { identifier ->
                journalist.logger.info("CLI | $identifier accessed remote console")
                val session = registerSession(sse, identifier)
                if (session == null) {
                    sse.close()
                    return@peek
                }
                if (sse.terminated()) {
                    closeSession(sse, session)
                    return@peek
                }
                journalist.cachedLogger.messages
                    .asSequence()
                    .filter { it.key.priority >= journalist.visibleThreshold.priority }
                    .takeWhile { !sse.terminated() }
                    .forEach { message -> sse.sendEvent(SSE_EVENT_NAME, message.value) }
            }
            .onError {
                journalist.logger.info("CLI | ${it.message} (${it.status})")
                sse.sendEvent(SSE_EVENT_NAME, it)
                sse.close()
            }
    }

    @Synchronized
    private fun registerSession(sse: SseClient, identifier: String): SseSession? {
        if (stopping) {
            return null
        }
        val completion = CompletableFuture<Void>()
        sse.ctx().future { completion }
        val subscriberId = journalist.subscribe {
            if (!sse.terminated()) {
                sse.sendEvent(SSE_EVENT_NAME, it.value)
            }
        }
        val watcher = scheduler.scheduleWithFixedDelay({
            if (!sse.terminated()) {
                sse.sendComment("ping")
            }
        }, 5, 5, TimeUnit.SECONDS)
        val session = SseSession(identifier, subscriberId, watcher, completion)
        sessions[sse] = session
        sse.onClose { closeSession(sse, session) }
        return session
    }

    private fun authenticateContext(connection: Context): Result<String, ErrorResponse> {
        return extractFromHeader(connection.header(Header.AUTHORIZATION))
            .map { (name, secret) ->
                Credentials(
                    ip = connection.ip(),
                    name = name,
                    secret = secret
                )
            }
            .flatMap { authenticationFacade.authenticateByCredentials(it) }
            .filter(
                { accessTokenFacade.hasPermission(it.identifier, AccessTokenPermission.MANAGER) },
                { unauthorized("Unauthorized CLI access request from ${connection.ip()}") }
            )
            .map { "${it.name}@${connection.ip()}" }
    }
}
