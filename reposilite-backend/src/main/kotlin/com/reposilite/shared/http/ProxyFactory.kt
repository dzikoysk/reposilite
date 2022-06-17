package com.reposilite.shared.http

import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy

private class PasswordAuthenticator(val authentication: PasswordAuthentication) : Authenticator() {

    override fun getPasswordAuthentication(): PasswordAuthentication =
        authentication

}

/**
 * Parses proxy configuration in the following format:
 * <PROXY-TYPE> <HOST>:<PORT> <LOGIN> <PASSWORD>
 */
fun createHttpProxy(configuration: String): Proxy? {
    if (configuration.isEmpty()) {
        return null
    }

    val protocol = Proxy.Type.valueOf(configuration.substringBefore(" ").uppercase())
    var addressConfiguration = configuration.substringAfter(" ")

    if (addressConfiguration.contains(" ")) {
        val credentials = addressConfiguration.substringAfter(" ")
        addressConfiguration = addressConfiguration.substringBefore(" ")

        val (login, password) = credentials.split(" ")
        val authentication = PasswordAuthentication(login, password.toCharArray())
        Authenticator.setDefault(PasswordAuthenticator(authentication))
    }

    val address = InetSocketAddress(
        addressConfiguration.substringBeforeLast(":"),
        addressConfiguration.substringAfterLast(":").toInt()
    )

    return Proxy(protocol, address)
}
