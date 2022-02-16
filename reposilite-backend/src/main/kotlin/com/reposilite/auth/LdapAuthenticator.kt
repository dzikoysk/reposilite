package com.reposilite.auth

import com.reposilite.auth.api.AuthenticationRequest
import com.reposilite.settings.api.SharedConfiguration.LdapConfiguration
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenType.PERSISTENT
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

internal class LdapAuthenticator(
    private val ldapConfiguration: LdapConfiguration,
    private val accessTokenFacade: AccessTokenFacade
) : Authenticator {

    override fun authenticate(authenticationRequest: AuthenticationRequest): Result<AccessTokenDto, ErrorResponse> =
        createSearchContext()
            .flatMap { it.search("(&(objectClass=person)(${ldapConfiguration.usernameAttribute}=${authenticationRequest.name}))", ldapConfiguration.usernameAttribute) } // find user entry with search user
            .filter({ it.size == 1 }, { ErrorResponse(BAD_REQUEST, "Could not identify one specific result") }) // only one search result allowed
            .map { it.first() }
            .flatMap { createContext(user = it.first, password = authenticationRequest.secret) } // try to authenticate user with matched domain namespace
            .flatMap { it.search(ldapConfiguration.userFilter, ldapConfiguration.usernameAttribute) } // filter result with user-filter from configuration
            .filter({ it.size == 1 }, { ErrorResponse(BAD_REQUEST, "Could not identify one specific result") }) // only one search result allowed
            .map { it.first() }
            .map { (_, attributes) -> attributes[ldapConfiguration.usernameAttribute]!! } // search returns only lists with values
            .filter({ it.size == 1 }, { ErrorResponse(BAD_REQUEST, "Could not identify one specific attribute") }) // only one attribute value is allowed
            .map { it.first() }
            .filter({ authenticationRequest.name == it }, { unauthorized("LDAP user does not match required attribute") }) // make sure requested name matches required attribute
            .map { name ->
                accessTokenFacade.getAccessToken(name)
                    ?: accessTokenFacade.createAccessToken(
                        CreateAccessTokenRequest(
                                type = TEMPORARY,
                                name = name,
                                secret = authenticationRequest.secret
                        )
                    ).accessToken
            }

    private fun createSearchContext(): Result<out DirContext, ErrorResponse> =
        createContext(user = ldapConfiguration.searchUserDn, password = ldapConfiguration.searchUserPassword)

    private fun createContext(
        hostname: String = ldapConfiguration.hostname,
        port: Int = ldapConfiguration.port,
        user: String,
        password: String
    ): Result<out DirContext, ErrorResponse> =
        Hashtable<String, String>()
            .also {
                it[INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
                it[PROVIDER_URL] = "ldap://$hostname:$port"
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

    private fun DirContext.search(ldapFilterQuery: String, vararg requestedAttributes: String): Result<List<Pair<String, Map<String, List<String>>>>, ErrorResponse> {
        return try {
            val searchControls = SearchControls().also {
                it.returningAttributes = requestedAttributes
                it.searchScope = SearchControls.SUBTREE_SCOPE
            }

            val searchResults = search(ldapConfiguration.baseDn, ldapFilterQuery, searchControls);
            val result = mutableListOf<Pair<String, Map<String, List<String>>>>()

            if (searchResults.hasMore()) {
                val resultEntry = searchResults.next()
                val attributes = resultEntry.attributes
                val mappedAttributes = mutableMapOf<String, MutableList<String>>()

                requestedAttributes.forEach { attribute ->
                    attributes.get(attribute)
                        ?.all
                        ?.let { attributeValue ->
                            mutableListOf<String>().also {
                                while (attributeValue.hasMore()) {
                                    it.add(attributeValue.next().toString())
                                }
                                attributeValue.close()
                            }
                        }
                        ?.also { mappedAttributes[attribute] = it }
                }

                if (mappedAttributes.isEmpty())
                    return notFoundError("Entry does not contain requested attributes")
                else
                    result.add(Pair(resultEntry.nameInNamespace, mappedAttributes))
            }

            if (result.isEmpty())
                notFoundError("Entries not found")
            else
                result.asSuccess()
        } catch (nameNotFoundException: NameNotFoundException) {
            notFoundError(nameNotFoundException.message ?: nameNotFoundException.javaClass.toString())
        } catch (invalidSearchFilterException: InvalidSearchFilterException) {
            errorResponse(BAD_REQUEST, invalidSearchFilterException.message ?: invalidSearchFilterException.javaClass.toString())
        }
    }

    override fun realm(): String =
            "LDAP"

}