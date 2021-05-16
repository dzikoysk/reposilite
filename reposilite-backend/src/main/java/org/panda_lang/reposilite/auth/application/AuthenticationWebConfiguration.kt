package org.panda_lang.reposilite.auth.application

import net.dzikoysk.dynamiclogger.Journalist
import org.panda_lang.reposilite.Reposilite
import org.panda_lang.reposilite.auth.AuthenticationFacade
import org.panda_lang.reposilite.auth.AuthenticationService
import org.panda_lang.reposilite.auth.Authenticator
import org.panda_lang.reposilite.auth.ChAliasCommand
import org.panda_lang.reposilite.auth.ChmodCommand
import org.panda_lang.reposilite.auth.KeygenCommand
import org.panda_lang.reposilite.auth.RevokeCommand
import org.panda_lang.reposilite.auth.SessionService
import org.panda_lang.reposilite.auth.TokensCommand
import org.panda_lang.reposilite.auth.infrastructure.AuthenticationEndpoint
import org.panda_lang.reposilite.auth.infrastructure.PostAuthHandler
import org.panda_lang.reposilite.console.ConsoleFacade
import org.panda_lang.reposilite.maven.MavenFacade
import org.panda_lang.reposilite.token.AccessTokenFacade
import org.panda_lang.reposilite.token.infrastructure.SqlAccessTokenRepository

object AuthenticationWebConfiguration {

    fun create(journalist: Journalist, mavenFacade: MavenFacade): AuthenticationFacade {
        val accessTokenFacade = AccessTokenFacade(journalist, SqlAccessTokenRepository())
        val authenticator = Authenticator(accessTokenFacade)
        val sessionService = SessionService(mavenFacade)
        val authService = AuthenticationService(authenticator, sessionService)

        return AuthenticationFacade(journalist, authService, accessTokenFacade)
    }

    fun initialize(consoleFacade: ConsoleFacade, authenticationFacade: AuthenticationFacade) {
        with(consoleFacade) {
            registerCommand(ChAliasCommand(authenticationFacade.accessTokenFacade))
            registerCommand(ChmodCommand(authenticationFacade.accessTokenFacade))
            registerCommand(KeygenCommand(authenticationFacade.accessTokenFacade))
            registerCommand(RevokeCommand(authenticationFacade.accessTokenFacade))
            registerCommand(TokensCommand(authenticationFacade.accessTokenFacade))
        }
    }

    fun installRoutes(reposilite: Reposilite) =
        reposilite.httpServer.javalin.get()
                .get("/api/auth", AuthenticationEndpoint(reposilite.authenticationFacade))
                .after("/*", PostAuthHandler())

}