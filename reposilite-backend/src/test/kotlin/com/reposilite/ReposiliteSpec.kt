package com.reposilite

import com.reposilite.journalist.Channel
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

internal abstract class ReposiliteSpec {

    companion object {
        val portAssigner = AtomicInteger(1025)
    }

    @TempDir
    lateinit var reposiliteWorkingDirectory: File
    protected lateinit var reposilite: Reposilite

    private var port: Int = -1

    protected val base: String
        get() = "http://localhost:$port"

    @BeforeEach
    protected fun bootApplication() = runBlocking {
        // disable log.txt to avoid conflicts with parallel testing
        System.setProperty("tinylog.writerFile.level", "off")

        val parameters = ReposiliteParameters()

        port = portAssigner.incrementAndGet()
        parameters.port = port

        val (name, secret) = useAuth()
        parameters.tokenEntries = arrayOf("$name:$secret")

        parameters.workingDirectoryName = reposiliteWorkingDirectory.absolutePath
        parameters.testEnv = true
        parameters.run()

        reposilite = ReposiliteFactory.createReposilite(parameters)
        reposilite.journalist.setVisibleThreshold(Channel.WARN)
        reposilite.launch()
    }

    @AfterEach
    protected fun shutdownApplication() {
        reposilite.shutdown()
    }

    fun useAuth(): Pair<String, String> =
        Pair("manager", "manager-secret")

    suspend fun useAuth(name: String, secret: String, routes: Map<String, RoutePermission> = emptyMap()): Pair<String, String> {
        val accessTokenFacade = reposilite.accessTokenFacade
        var accessToken = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(name, secret)).accessToken

        routes.forEach { (route, permission) ->
            accessToken = accessTokenFacade.updateToken(accessToken.withRoute(Route(route, setOf(permission))))
        }

        return Pair(name, secret)
    }

}