package com.reposilite.auth

import com.reposilite.auth.api.AuthenticationRequest
import com.reposilite.settings.api.SharedConfiguration.LdapConfiguration
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.notFoundError
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
    private val ldapConfiguration: LdapConfiguration
) : Authenticator {

    override fun authenticate(authenticationRequest: AuthenticationRequest): Result<AccessTokenDto, ErrorResponse> {
        TODO("Not yet implemented")
    }

    fun search(ldapFilterQuery: String, vararg requestedAttributes: String): Result<List<Map<String, List<String>>>, ErrorResponse> {
        return with(ldapConfiguration) {
            createContext(hostname, port, searchUserDn, searchUserPassword)
        }.flatMap { context ->
            try {
                val searchControls = SearchControls().also {
                    it.returningAttributes = requestedAttributes
                    it.searchScope = SearchControls.SUBTREE_SCOPE
                }

                val searchResults = context.search(ldapConfiguration.baseDn, ldapFilterQuery, searchControls);
                val result = mutableListOf<Map<String, List<String>>>()

                if (searchResults.hasMore()) {
                    val resultEntry = searchResults.next()
                    val attributes = resultEntry.attributes
                    val mappedAttributes = mutableMapOf<String, MutableList<String>>()

                    requestedAttributes.forEach { attribute ->
                        mappedAttributes[attribute] = attributes.get(attribute).all.let {
                            val list = mutableListOf<String>()

                            while (it.hasMore()) {
                                val next=  it.next()
                                println(next)
                                list.add(next.toString())
                            }

                            it.close()
                            list
                        }
                    }

                    result.add(mappedAttributes)
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
    }

    private fun createContext(hostname: String, port: Int, user: String, password: String): Result<out DirContext, ErrorResponse> =
        Hashtable<String, String>()
            .also {
                it[INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
                it[PROVIDER_URL] = "ldap://$hostname:$port"
                it[SECURITY_AUTHENTICATION] = "simple"
                it[SECURITY_PRINCIPAL] = user
                it[SECURITY_CREDENTIALS] = password
            }
            .let { Result.attempt { InitialDirContext(it) } }
            .mapErr { ErrorResponse(UNAUTHORIZED, "Unauthorized LDAP access") }

    override fun name(): String =
            "LDAP"

}