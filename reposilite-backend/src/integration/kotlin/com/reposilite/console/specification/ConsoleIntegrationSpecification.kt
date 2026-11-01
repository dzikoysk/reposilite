/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.console.specification

import com.reposilite.ReposiliteSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

internal abstract class ConsoleIntegrationSpecification : ReposiliteSpecification() {

    private val connections = mutableListOf<HttpURLConnection>()

    protected fun useLog(message: String) {
        LoggerFactory.getLogger(javaClass).warn(message)
    }

    protected fun useConsole(): BufferedReader {
        useLog("console-ready")
        val (name, secret) = useDefaultManagementToken()
        val authorization = Base64.getEncoder().encodeToString("$name:$secret".toByteArray())
        val connection = (URI("$base/api/console/log").toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("Authorization", "Basic $authorization")
        }
        connections.add(connection)
        assertThat(connection.responseCode).isEqualTo(200)
        return connection.inputStream.bufferedReader().also { reader ->
            val event = generateSequence { reader.readLine() }.firstOrNull { it.contains("console-ready") }
            assertThat(event).isNotNull()
        }
    }

    @AfterEach
    fun closeConsoles() {
        connections.forEach(HttpURLConnection::disconnect)
    }

}
