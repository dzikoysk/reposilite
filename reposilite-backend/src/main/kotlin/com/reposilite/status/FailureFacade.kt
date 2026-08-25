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

package com.reposilite.status

import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import com.reposilite.status.api.RecordedFailure
import java.util.concurrent.ConcurrentHashMap

class FailureFacade(private val journalist: Journalist) : Journalist, Facade {

    private val exceptions = ConcurrentHashMap<String, CachedFailure>()

    fun throwException(identifier: String, throwable: Throwable) =
        throwException(identifier, Channel.ERROR, throwable)

    private fun throwException(identifier: String, channel: Channel, throwable: Throwable) {
        logger.log(channel, identifier)
        logger.exception(channel, throwable)

        val message = throwable.message.orEmpty()
        val trace = arrayOf("failure $identifier", exceptionToString(throwable))
            .joinToString(separator = System.lineSeparator())
            .trim()

        exceptions.compute(failureSignature(identifier, throwable)) { _, cachedFailure ->
            cachedFailure?.copy(
                messages = if (message.isBlank()) cachedFailure.messages else cachedFailure.messages + message,
                occurrences = cachedFailure.occurrences + 1
            )
                ?: CachedFailure(
                    path = identifier,
                    type = throwable.javaClass.simpleName,
                    message = message,
                    trace = trace
                )
        }
    }

    private fun failureSignature(identifier: String, throwable: Throwable): String =
        generateSequence(throwable) { it.cause }
            .joinToString(prefix = "$identifier|", separator = "|") {
                "${it.javaClass.name}@${it.stackTrace.firstOrNull() ?: "<unknown stacktrace>"}"
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
            .take(5)
            .joinToString(prefix = "  at ", separator = System.lineSeparator())
            .takeIf { it.isNotEmpty() }
            ?: "<unknown stacktrace>"

    fun hasFailures() =
        exceptions.isNotEmpty()

    fun getFailuresCount(): Int =
        exceptions.values.sumOf { it.occurrences }

    fun getFailures(): Collection<String> =
        getRecordedFailures().map { it.trace }

    fun getRecordedFailures(): Collection<RecordedFailure> =
        exceptions.values.map { it.toRecordedFailure() }

    private data class CachedFailure(
        val path: String,
        val type: String,
        val message: String,
        val trace: String,
        val messages: Set<String> = if (message.isBlank()) emptySet() else setOf(message),
        val occurrences: Int = 1
    ) {

        fun toRecordedFailure(): RecordedFailure =
            RecordedFailure(
                path = path,
                type = type,
                message = message,
                messages = messages.toList(),
                trace = trace,
                occurrences = occurrences
            )

    }

    override fun getLogger(): Logger =
        journalist.logger

}
