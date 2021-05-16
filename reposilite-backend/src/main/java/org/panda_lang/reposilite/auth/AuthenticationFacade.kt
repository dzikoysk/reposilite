package org.panda_lang.reposilite.auth

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.panda_lang.reposilite.auth.api.AuthenticationResponse
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.token.AccessTokenFacade
import org.panda_lang.utilities.commons.function.Result

class AuthenticationFacade internal constructor(
    private val journalist: Journalist,
    private val authenticationService: AuthenticationService,
    internal val accessTokenFacade: AccessTokenFacade,
) : Journalist {

    fun authenticateByHeader(headers: Map<String, String>): Result<AuthenticationResponse, ErrorResponse> =
        authenticationService.authByHeader(headers)

    override fun getLogger(): Logger =
        journalist.logger

}