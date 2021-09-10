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
import java.util.concurrent.ThreadLocalRandom
import kotlin.Short.Companion.MAX_VALUE

internal abstract class ReposiliteSpec {

    @TempDir
    lateinit var reposiliteWorkingDirectory: File
    protected lateinit var reposilite: Reposilite

    private var port: Int = -1

    protected val base: String
        get() = "http://localhost:$port"

    @BeforeEach
    protected fun bootApplication() = runBlocking {
        System.setProperty("tinylog.writerFile.level", "off") // disable log.txt to avoid conflicts with parallel testing
        tryCreateReposilite()
    }

    @AfterEach
    protected fun shutdownApplication() {
        reposilite.shutdown()
    }

    private suspend fun tryCreateReposilite() {
        try {
            createReposilite()
        } catch (portUnavailable : RuntimeException) {
            if (portUnavailable.message?.contains("port") == true) {
                tryCreateReposilite()
            }
            else {
                portUnavailable.printStackTrace()
            }
        }
    }

    private suspend fun createReposilite() {
        val parameters = ReposiliteParameters()

        port = ThreadLocalRandom.current().nextInt(1025, MAX_VALUE - 1025)
        parameters.port = port

        val (name, secret) = useAuth()
        parameters.tokenEntries = arrayOf("$name:$secret")

        parameters.workingDirectoryName = reposiliteWorkingDirectory.absolutePath
        parameters.testEnv = true
        parameters.run()

        reposilite = ReposiliteFactory.createReposilite(parameters)
        reposilite.journalist.setVisibleThreshold(Channel.DEBUG)
        reposilite.launch()
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