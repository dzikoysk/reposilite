/*
 * Copyright (c) 2023 dzikoysk
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

import com.reposilite.auth.api.Credentials
import com.reposilite.auth.application.LdapSettings
import com.reposilite.journalist.Channel.DEBUG
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequest
import com.reposilite.shared.badRequestError
import com.reposilite.shared.extensions.accept
import com.reposilite.shared.internalServerError
import com.reposilite.shared.notFoundError
import com.reposilite.shared.unauthorized
import com.reposilite.status.FailureFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import panda.std.Result
import panda.std.Result.supplyThrowing
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

data class SearchEntry(
    val fullName: String,
    val attributes: AttributesMap
)

internal class LdapAuthenticator(
    private val ldapSettings: Reference<LdapSettings>,
    private val accessTokenFacade: AccessTokenFacade,
    private val failureFacade: FailureFacade
) : Authenticator {

    private val protocol = when {
        ldapSettings.get().ssl -> "ldaps"
        else -> "ldap"
    }

    override fun authenticate(credentials: Credentials): Result<AccessTokenDto, ErrorResponse> =
        with(ldapSettings.get()) {
            createSearchContext()
                .flatMap {
                    it.search(
                        ldapFilterQuery = "(&(objectClass=person)($userAttribute={0}))", // find user entry with search user,
                        ldapFilterQueryArguments = arrayOf(credentials.name),
                        requestedAttributes = arrayOf(userAttribute)
                    )
                }
                .filter { users ->
                    when {
                        users.isEmpty() -> badRequest("User not found")
                        users.size > 1 -> badRequest("Could not identify one specific result")
                        else -> accept() // only one search result allowed
                    }
                }
                .map { users -> users.first() }
                .flatMap { matchedUserObject ->
                    // try to authenticate user with matched domain namespace
                    createDirContext(
                        user = matchedUserObject.fullName,
                        password = credentials.secret
                    )
                }
                .flatMap {
                    it.search(
                        ldapFilterQuery = "(&(objectClass=person)($userAttribute={0})$userFilter)", // filter result with user-filter from configuration
                        ldapFilterQueryArguments = arrayOf(credentials.name),
                        requestedAttributes = arrayOf(userAttribute)
                    )
                }
                .filter { filterResults ->
                    when {
                        filterResults.isEmpty() -> badRequest("User matching filter not found")
                        filterResults.size > 1 -> badRequest("Could not identify one specific result with specified user-filter")
                        else -> accept() // only one search result allowed
                    }
                }
                .map { filterResults -> filterResults.first() }
                .map { it.attributes[userAttribute]!! } // search returns only lists with values
                .filter { usernameAttributeObject ->
                    when {
                        usernameAttributeObject.isEmpty() -> badRequest("Username attribute not found")
                        usernameAttributeObject.size > 1 -> badRequest("Could not identify one specific username attribute: ${usernameAttributeObject.joinToString()}")
                        else -> accept() // only one attribute value is allowed
                    }
                }
                .map { attributes -> attributes.first() }
                .filter { username ->
                    when {
                        username != credentials.name -> unauthorized("LDAP user does not match required attribute")
                        else -> accept()
                    }
                }
                .map { username ->
                    accessTokenFacade.getAccessToken(username)
                        ?: accessTokenFacade.createAccessToken(
                            CreateAccessTokenRequest(
                                type = ldapSettings.map { it.userType },
                                name = username,
                                secret = credentials.secret
                            )
                        ).accessToken
                }
        }

    private fun createDirContext(user: String, password: String): Result<out DirContext, ErrorResponse> =
        Hashtable<String, String>()
            .also {
                it[INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
                it[PROVIDER_URL] = with(ldapSettings.get()) { "$protocol://$hostname:$port" }
                it[SECURITY_AUTHENTICATION] = "simple"
                it[SECURITY_PRINCIPAL] = user
                it[SECURITY_CREDENTIALS] = password
            }
            .let { supplyThrowing { InitialDirContext(it) } }
            .mapErr {
                accessTokenFacade.logger.exception(DEBUG, it)
                unauthorized("Unauthorized LDAP access")
            }

    fun search(
        ldapFilterQuery: String,
        ldapFilterQueryArguments: Array<Any>,
        requestedAttributes: Array<out String>
    ): Result<List<SearchEntry>, ErrorResponse> =
        createSearchContext().flatMap {
            it.search(
                ldapFilterQuery = ldapFilterQuery,
                ldapFilterQueryArguments = ldapFilterQueryArguments,
                requestedAttributes = requestedAttributes
            )
        }

    private fun createSearchContext(): Result<out DirContext, ErrorResponse> =
        ldapSettings.map {
            createDirContext(
                user = it.searchUserDn,
                password = it.searchUserPassword
            )
        }

    private fun DirContext.search(
        ldapFilterQuery: String,
        ldapFilterQueryArguments: Array<Any>,
        requestedAttributes: Array<out String>
    ): Result<List<SearchEntry>, ErrorResponse> =
        try {
            SearchControls()
                .also {
                    it.searchScope = SearchControls.SUBTREE_SCOPE
                    it.returningAttributes = requestedAttributes
                }
                .let {
                    this.search(
                        ldapSettings.get().baseDn,
                        ldapFilterQuery,
                        ldapFilterQueryArguments,
                        it
                    )
                }
                .asSequence()
                .map {
                    SearchEntry(
                        fullName = it.nameInNamespace,
                        attributes = it.attributesMap(requestedAttributes)
                    )
                }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.asSuccess()
                ?: notFoundError("Entries not found")
        } catch (nameNotFoundException: NameNotFoundException) {
            notFoundError(nameNotFoundException.toString())
        } catch (invalidSearchFilterException: InvalidSearchFilterException) {
            failureFacade.throwException("Bad search request in LDAP", invalidSearchFilterException)
            badRequestError(invalidSearchFilterException.toString())
        } catch (exception: Exception) {
            failureFacade.throwException("Unknown LDAP search exception", exception)
            internalServerError(exception.toString())
        }

    private fun SearchResult.attributesMap(requestedAttributes: Array<out String>): AttributesMap =
        requestedAttributes.associate { attribute ->
            attributes.get(attribute)
                .all
                .asSequence()
                .map { it.toString() }
                .toList()
                .let { attribute to it }
        }

    override fun enabled(): Boolean =
        ldapSettings.map { it.enabled }

    override fun realm(): String =
        "LDAP"

}
