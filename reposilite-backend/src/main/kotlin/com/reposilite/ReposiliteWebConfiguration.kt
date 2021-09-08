package com.reposilite

import com.reposilite.auth.application.AuthenticationWebConfiguration
import com.reposilite.console.application.ConsoleWebConfiguration
import com.reposilite.failure.application.FailureWebConfiguration
import com.reposilite.frontend.application.FrontendWebConfiguration
import com.reposilite.maven.application.MavenWebConfiguration
import com.reposilite.statistics.application.StatisticsWebConfiguration
import com.reposilite.token.application.AccessTokenWebConfiguration
import com.reposilite.web.ReposiliteRoutes
import io.javalin.Javalin

object ReposiliteWebConfiguration {

    fun initialize(reposilite: Reposilite) {
        // AuthenticationWebConfiguration.initialize()
        FailureWebConfiguration.initialize(reposilite.consoleFacade, reposilite.failureFacade)
        ConsoleWebConfiguration.initialize(reposilite.consoleFacade, reposilite)
        // MavenWebConfiguration.initialize()
        // FrontendWebConfiguration.initialize()
        // MavenWebConfiguration.initialize()
        StatisticsWebConfiguration.initialize(reposilite.statisticsFacade, reposilite.consoleFacade, reposilite.scheduler, reposilite.dispatcher)
        AccessTokenWebConfiguration.initialize(reposilite.accessTokenFacade, reposilite.parameters.tokens, reposilite.consoleFacade)
    }

    fun routing(reposilite: Reposilite): List<ReposiliteRoutes> =
        setOf(
            AuthenticationWebConfiguration.routing(reposilite.authenticationFacade),
            ConsoleWebConfiguration.routing(reposilite),
            FailureWebConfiguration.routing(),
            FrontendWebConfiguration.routing(reposilite.frontendFacade, reposilite.parameters.workingDirectory),
            MavenWebConfiguration.routing(reposilite.mavenFacade, reposilite.frontendFacade),
            StatisticsWebConfiguration.routing(reposilite.statisticsFacade),
            AccessTokenWebConfiguration.routing(),
        ).flatten()

    // TOFIX: Remove dependency on infrastructure details
    fun javalin(reposilite: Reposilite, javalin: Javalin) {
        ConsoleWebConfiguration.javalin(javalin, reposilite)
        FailureWebConfiguration.javalin(javalin, reposilite.failureFacade)
        FrontendWebConfiguration.javalin(javalin, reposilite.frontendFacade)
    }

    fun dispose(reposilite: Reposilite) {
        ConsoleWebConfiguration.dispose(reposilite.consoleFacade)
    }

}