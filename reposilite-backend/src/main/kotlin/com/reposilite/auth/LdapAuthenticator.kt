package com.reposilite.auth

import com.reposilite.auth.api.AuthenticationRequest
import com.reposilite.settings.api.SharedConfiguration.LdapConfiguration
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.notFoundError
import com.reposilite.web.http.unauthorized
import io.javalin.http.HttpCode.BAD_REQUEST
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

internal class LdapAuthenticator(
    private val ldapConfiguration: Reference<LdapConfiguration>,
    private val accessTokenFacade: AccessTokenFacade
) : Authenticator {

    override fun authenticate(authenticationRequest: AuthenticationRequest): Result<AccessTokenDto, ErrorResponse> =
        with(ldapConfiguration.get()) {
            createSearchContext()
                .flatMap {
                    it.search(
                        "(&(objectClass=person)(${usernameAttribute}=${authenticationRequest.name}))", // find user entry with search user
                        usernameAttribute
                    )
                }
                .filter({ it.size == 1 }, { ErrorResponse(BAD_REQUEST, "Could not identify one specific result") }) // only one search result allowed
                .map { it.first() }
                .flatMap { createContext(user = it.first, password = authenticationRequest.secret) } // try to authenticate user with matched domain namespace
                .flatMap { it.search(userFilter, usernameAttribute) } // filter result with user-filter from configuration
                .filter({ it.size == 1 }, { ErrorResponse(BAD_REQUEST, "Could not identify one specific result") }) // only one search result allowed
                .map { it.first() }
                .map { (_, attributes) -> attributes[usernameAttribute]!! } // search returns only lists with values
                .filter({ it.size == 1 }, { ErrorResponse(BAD_REQUEST, "Could not identify one specific attribute") }) // only one attribute value is allowed
                .map { it.first() }
                .filter(
                    { authenticationRequest.name == it }, // make sure requested name matches required attribute
                    { unauthorized("LDAP user does not match required attribute") }
                )
                .map { name -> accessTokenFacade.getAccessToken(name)
                    ?: accessTokenFacade.createAccessToken(
                        CreateAccessTokenRequest(
                            type = TEMPORARY,
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
                it[PROVIDER_URL] = ldapConfiguration.map { "ldap://${it.hostname}:${it.port}" }
                it[SECURITY_AUTHENTICATION] = "simple"
                it[SECURITY_PRINCIPAL] = user
                it[SECURITY_CREDENTIALS] = password
            }
            .let { Result.attempt { InitialDirContext(it) } }
            .mapErr {
                it.printStackTrace()
                ErrorResponse(UNAUTHORIZED, "Unauthorized LDAP access")
            }

    fun search(ldapFilterQuery: String, vararg requestedAttributes: String): Result<List<Pair<String, Map<String, List<String>>>>, ErrorResponse> =
        createSearchContext()
            .flatMap { it.search(ldapFilterQuery, *requestedAttributes) }

    private fun DirContext.search(ldapFilterQuery: String, vararg requestedAttributes: String): Result<List<Pair<String, Map<String, List<String>>>>, ErrorResponse> =
        try {
            SearchControls()
                .also {
                    it.returningAttributes = requestedAttributes
                    it.searchScope = SearchControls.SUBTREE_SCOPE
                }
                .let { search(ldapConfiguration.map { it.baseDn }, ldapFilterQuery, it) }
                .asSequence()
                .map { Pair(it.nameInNamespace, it.attributesMap(*requestedAttributes)) }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.asSuccess()
                ?: notFoundError("Entries not found")
        } catch (nameNotFoundException: NameNotFoundException) {
            notFoundError(nameNotFoundException.message ?: nameNotFoundException.javaClass.toString())
        } catch (invalidSearchFilterException: InvalidSearchFilterException) {
            errorResponse(BAD_REQUEST, invalidSearchFilterException.message ?: invalidSearchFilterException.javaClass.toString())
        }

    private fun SearchResult.attributesMap(vararg requestedAttributes: String): Map<String, List<String>> =
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