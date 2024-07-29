package com.reposilite.console.infrastructure

import com.reposilite.ReposiliteJournalist
import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.api.Credentials
import com.reposilite.console.ConsoleFacade
import com.reposilite.console.api.ExecutionResponse
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
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import panda.std.Result
import panda.std.reactive.Reference
import java.nio.channels.ClosedChannelException
import java.util.*
import java.util.function.Consumer

private const val SSE_EVENT_NAME = "log"

private data class SseSession(
    val identifier: String,
    val subscriberId: Int,
)

internal class ConsoleSseHandler(
    private val journalist: ReposiliteJournalist,
    private val accessTokenFacade: AccessTokenFacade,
    private val authenticationFacade: AuthenticationFacade,
    private val consoleFacade: ConsoleFacade,
    private val forwardedIp: Reference<String>
) : Consumer<SseClient> {

    private val users: WeakHashMap<SseClient, SseSession> = WeakHashMap()

    @OpenApi(
        // TODO: does this need better endpoint name?
        path = "/api/console/log",
        methods = [HttpMethod.GET],
        // TODO: the responses parameter is *technically* not really needed, but would be nice to have
        responses = [
            OpenApiResponse(
                status = "200",
                // TODO: better description?
                description = "Continuously sends out the log as data messages",
                content = [OpenApiContent(from = ExecutionResponse::class)]
            )
        ],
        tags = ["Console"]
    )
    override fun accept(sse: SseClient) {
        sse.keepAlive()
        sse.onClose {  ->
            val session = users.remove(sse) ?: return@onClose
            journalist.logger.info("CLI | ${session.identifier} closed connection")
            journalist.unsubscribe(session.subscriberId)
        }

        when (users[sse]) {
            null ->
                authenticateContext(sse.ctx())
                    .peek { identifier ->
                        journalist.logger.info("CLI | $identifier accessed remote console")

                        val subscriberId = journalist.subscribe {
                            // TODO: do better
                            try {
                                if (!sse.terminated()) {
                                    sse.sendEvent(SSE_EVENT_NAME, it.value)
                                } else {
                                    sse.close()
                                }
                            } catch (ignored: ClosedChannelException) {
                                sse.close()
                            } catch (ignored: InterruptedException) {
                                sse.close()
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
    }

    private fun authenticateContext(connection: Context): Result<String, ErrorResponse> {
        return extractFromHeader(connection.header(Header.AUTHORIZATION))
            .map { (name, secret) ->
                Credentials(
                    host = connection.getHost(),
                    name = name,
                    secret = secret
                )
            }
            .flatMap { authenticationFacade.authenticateByCredentials(it) }
            .filter(
                { accessTokenFacade.hasPermission(it.identifier, AccessTokenPermission.MANAGER) },
                { unauthorized("Unauthorized CLI access request from ${connection.getHost()}") }
            )
            .map { "${it.name}@${connection.getHost()}" }
    }

    private fun Context.getHost(): String =
        header(forwardedIp.get()) ?: req().remoteAddr
}
