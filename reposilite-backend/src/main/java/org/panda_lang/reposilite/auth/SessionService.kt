package org.panda_lang.reposilite.auth

import org.panda_lang.reposilite.maven.MavenFacade
import org.panda_lang.reposilite.token.api.AccessToken

internal class SessionService(private val mavenFacade: MavenFacade) {

    fun createSession(accessToken: AccessToken) =
        Session(accessToken)

}