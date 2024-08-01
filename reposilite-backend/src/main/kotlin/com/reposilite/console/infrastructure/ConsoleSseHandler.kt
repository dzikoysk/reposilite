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
import java.util.*
import java.util.function.Consumer

private const val SSE_EVENT_NAME = "log"

data class SseSession(
    val identifier: String,
    val subscriberId: Int,
)

internal class ConsoleSseHandler(
    private val journalist: ReposiliteJournalist,
    private val accessTokenFacade: AccessTokenFacade,
    private val authenticationFacade: AuthenticationFacade,
    private val forwardedIp: Reference<String>
) {

    internal val users: WeakHashMap<SseClient, SseSession> = WeakHashMap()

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
        sse.keepAlive()
        sse.onClose {  ->
            val session = users.remove(sse) ?: return@onClose
            journalist.logger.info("CLI | ${session.identifier} closed connection")
            journalist.unsubscribe(session.subscriberId)
        }

        authenticateContext(sse.ctx())
            .peek { identifier ->
                journalist.logger.info("CLI | $identifier accessed remote console")

                val subscriberId = journalist.subscribe {
                    // stop stack overflow log spam
                    if (!sse.terminated()) {
                        sse.sendEvent(SSE_EVENT_NAME, it.value)
                    }
                }

                users[sse] = SseSession(identifier, subscriberId)

                journalist.cachedLogger.messages.forEach { message ->
                    sse.sendEvent(SSE_EVENT_NAME, message.value)
                }
            }
            .onError {
                journalist.logger.info("CLI | ${it.message} (${it.status})")
                sse.sendEvent(SSE_EVENT_NAME, it)
                sse.close()
            }
    }

    private fun authenticateContext(connection: Context): Result<String, ErrorResponse> {
        return extractFromHeader(connection.header(Header.AUTHORIZATION))
            .map { (name, secret) ->
                Credentials(
                    host = connection.ip(),
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
