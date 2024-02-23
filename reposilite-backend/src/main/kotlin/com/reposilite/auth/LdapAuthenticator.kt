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
import com.reposilite.journalist.Journalist
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
import panda.std.Result
import panda.std.Result.supplyThrowing
import panda.std.asSuccess
import panda.std.reactive.Reference

typealias Attributes = List<String>
typealias AttributesMap = Map<String, Attributes>

data class SearchEntry(
    val fullName: String,
    val attributes: AttributesMap
)

internal class LdapAuthenticator(
    private val journalist: Journalist,
    private val ldapSettings: Reference<LdapSettings>,
    private val accessTokenFacade: AccessTokenFacade,
    private val failureFacade: FailureFacade,
    private val disableUserPasswordAuthentication: Boolean = false,
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
                        ldapFilterQuery = "(&(objectClass=$typeAttribute)($userAttribute={0})$userFilter)", // find user entry with search user,
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
                .map { users ->
                    users.first().let {
                        it.fullName to it.attributes[userAttribute]
                    }
                }
                .filter({ (_, attributes) -> attributes != null }, { badRequest("User attribute not found") })
                .map { (userDomain, username) -> userDomain to username!! }
                .filter { (_, attributes) ->
                    when {
                        attributes.isEmpty() -> badRequest("Username attribute not found")
                        attributes.size > 1 -> badRequest("Could not identify one specific username attribute: ${attributes.joinToString()}")
                        else -> accept() // only one attribute value is allowed
                    }
                }
                .map { (userDomain, username) -> userDomain to username.first() }
                .filter({ (userDomain, _) -> disableUserPasswordAuthentication || createDirContext(user = userDomain, password = credentials.secret).isOk }) { unauthorized("Unauthorized LDAP access") }
                .map { (_, username) ->
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
                it.printStackTrace()
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
                    journalist.logger.debug(
                        "[LDAP] LDAP search query { name: %s, filter: %s, args: %s, cons: %s }".format(
                            ldapSettings.get().baseDn,
                            ldapFilterQuery,
                            ldapFilterQueryArguments.joinToString(", "),
                            "[${it.searchScope}, ${it.returningAttributes.joinToString(", ")}]"
                        )
                    )
                    this.search(
                        ldapSettings.get().baseDn,
                        ldapFilterQuery,
                        ldapFilterQueryArguments,
                        it
                    )
                }
                .asSequence()
                .onEach {
                    journalist.logger.debug("[LDAP] LDAP search result { fullName: %s, attributes: %s }".format(
                        it.nameInNamespace,
                        it.attributesMap(requestedAttributes)
                    ))
                }
                .map {
                    SearchEntry(
                        fullName = it.nameInNamespace,
                        attributes = it.attributesMap(requestedAttributes)
                    )
                }
                .toList()
                .also { journalist.logger.debug("[LDAP] LDAP search result: ${it.size} entries found") }
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
