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

package com.reposilite.status

import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import java.util.concurrent.ConcurrentHashMap

class FailureFacade(private val journalist: Journalist) : Journalist, Facade {

    private val exceptions = ConcurrentHashMap.newKeySet<String>()

    fun throwException(identifier: String, throwable: Throwable) =
        throwException(identifier, Channel.ERROR, throwable)

    private fun throwException(identifier: String, channel: Channel, throwable: Throwable) {
        logger.log(channel, identifier)
        logger.exception(channel, throwable)

        arrayOf("failure $identifier",exceptionToString(throwable))
            .joinToString(separator = System.lineSeparator())
            .trim()
            .let { exceptions.add(it) }
    }

    private fun exceptionToString(throwable: Throwable?): String =
        throwable?.let {
            arrayOf(
                "  by ${throwable.javaClass.simpleName}: ${throwable.message}",
                stacktraceToList(throwable),
                exceptionToString(throwable.cause)
            ).joinToString(separator = System.lineSeparator())
        } ?: ""

    private fun stacktraceToList(throwable: Throwable): String =
        throwable.stackTrace
            .take(3)
            .joinToString(prefix = "  at ", separator = System.lineSeparator())
            .takeIf { it.isNotEmpty() }
            ?: "<unknown stacktrace>"

    fun hasFailures() =
        exceptions.isNotEmpty()

    fun getFailures(): Collection<String> =
        exceptions

    override fun getLogger(): Logger =
        journalist.logger

}
