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
