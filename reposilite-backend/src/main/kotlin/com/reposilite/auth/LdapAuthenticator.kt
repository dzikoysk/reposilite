/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.auth

import com.reposilite.auth.api.AuthenticationRequest
import com.reposilite.journalist.Channel.DEBUG
import com.reposilite.settings.api.SharedConfiguration.LdapConfiguration
import com.reposilite.status.FailureFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.notFoundError
import com.reposilite.web.http.unauthorized
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpCode.UNAUTHORIZED
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.util.Hashtable
import javax.naming.Context.INITIAL_CONTEXT_FACTORY
import javax.naming.Context.PROVIDER_URL
import javax.naming.Context.SECURITY_AUTHENTICATION
import javax.naming.Context.SECURITY_CREDENTIALS
import javax.naming.Context.SECURITY_PRINCIPAL
import javax.naming.NameNotFoundException
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext
import javax.naming.directory.InvalidSearchFilterException
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult

typealias Attributes = List<String>
typealias AttributesMap = Map<String, Attributes>
typealias SearchEntry = Pair<String, AttributesMap>

internal class LdapAuthenticator(
    private val ldapConfiguration: Reference<LdapConfiguration>,
    private val accessTokenFacade: AccessTokenFacade,
    private val failureFacade: FailureFacade
) : Authenticator {

    override fun authenticate(authenticationRequest: AuthenticationRequest): Result<AccessTokenDto, ErrorResponse> =
        with(ldapConfiguration.get()) {
            createSearchContext()
                .flatMap {
                    it.search(
                        "(&(objectClass=person)(${userAttribute}=${authenticationRequest.name}))", // find user entry with search user
                        userAttribute
                    )
                }
                .filter({ it.size == 1 }, { ErrorResponse(BAD_REQUEST, "Could not identify one specific result") }) // only one search result allowed
                .map { it.first() }
                .flatMap { createContext(user = it.first, password = authenticationRequest.secret) } // try to authenticate user with matched domain namespace
                .flatMap { it.search(userFilter, userAttribute) } // filter result with user-filter from configuration
                .filter({ it.size == 1 }, { ErrorResponse(BAD_REQUEST, "Could not identify one specific result") }) // only one search result allowed
                .map { it.first() }
                .map { (_, attributes) -> attributes[userAttribute]!! } // search returns only lists with values
                .filter({ it.size == 1 }, { ErrorResponse(BAD_REQUEST, "Could not identify one specific attribute") }) // only one attribute value is allowed
                .map { it.first() }
                .filter(
                    { authenticationRequest.name == it }, // make sure requested name matches required attribute
                    { unauthorized("LDAP user does not match required attribute") }
                )
                .map { name -> accessTokenFacade.getAccessToken(name)
                    ?: accessTokenFacade.createAccessToken(
                        CreateAccessTokenRequest(
                            type = ldapConfiguration.map { it.userType },
                            name = name,
                            secret = authenticationRequest.secret
                        )
                    ).accessToken
                }
        }

    private fun createSearchContext(): Result<out DirContext, ErrorResponse> =
        ldapConfiguration.map { createContext(user = it.searchUserDn, password = it.searchUserPassword) }

    private fun createContext(user: String, password: String): Result<out DirContext, ErrorResponse> =
        Hashtable<String, String>()
            .also {
                it[INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
                it[PROVIDER_URL] = with(ldapConfiguration.get()) { "ldap://${hostname}:${port}" }
                it[SECURITY_AUTHENTICATION] = "simple"
                it[SECURITY_PRINCIPAL] = user
                it[SECURITY_CREDENTIALS] = password
            }
            .let { Result.attempt { InitialDirContext(it) } }
            .mapErr {
                accessTokenFacade.logger.exception(DEBUG, it)
                ErrorResponse(UNAUTHORIZED, "Unauthorized LDAP access")
            }

    fun search(ldapFilterQuery: String, vararg requestedAttributes: String): Result<List<SearchEntry>, ErrorResponse> =
        createSearchContext()
            .flatMap { it.search(ldapFilterQuery, *requestedAttributes) }

    private fun DirContext.search(ldapFilterQuery: String, vararg requestedAttributes: String): Result<List<SearchEntry>, ErrorResponse> =
        try {
            SearchControls()
                .also {
                    it.returningAttributes = requestedAttributes
                    it.searchScope = SearchControls.SUBTREE_SCOPE
                }
                .let { controls -> search(ldapConfiguration.map { it.baseDn }, ldapFilterQuery, controls) }
                .asSequence()
                .map { Pair(it.nameInNamespace, it.attributesMap(*requestedAttributes)) }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.asSuccess()
                ?: notFoundError("Entries not found")
        } catch (nameNotFoundException: NameNotFoundException) {
            notFoundError(nameNotFoundException.toString())
        } catch (invalidSearchFilterException: InvalidSearchFilterException) {
            failureFacade.throwException("Bad search request in LDAP", invalidSearchFilterException)
            errorResponse(BAD_REQUEST, invalidSearchFilterException.toString())
        } catch (exception: Exception) {
            failureFacade.throwException("Unknown LDAP search exception", exception)
            errorResponse(INTERNAL_SERVER_ERROR, exception.toString())
        }

    private fun SearchResult.attributesMap(vararg requestedAttributes: String): AttributesMap =
        requestedAttributes.associate { attribute ->
            attributes.get(attribute)
                .all
                .asSequence()
                .map { it.toString() }
                .toList()
                .let { Pair(attribute, it) }
        }

    override fun enabled(): Boolean =
        ldapConfiguration.map { it.enabled }

    override fun realm(): String =
        "LDAP"

}