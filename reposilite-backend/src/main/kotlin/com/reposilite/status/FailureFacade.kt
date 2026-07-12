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

package com.reposilite.status

import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import com.reposilite.status.api.RecordedFailure
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class FailureFacade(private val journalist: Journalist) : Journalist, Facade {

    private val exceptions = ConcurrentHashMap<String, CachedFailure>()

    fun throwException(identifier: String, throwable: Throwable) =
        throwException(identifier, Channel.ERROR, throwable)

    private fun throwException(identifier: String, channel: Channel, throwable: Throwable) {
        logger.log(channel, identifier)
        logger.exception(channel, throwable)

        val trace = arrayOf("failure $identifier", exceptionToString(throwable))
            .joinToString(separator = System.lineSeparator())
            .trim()

        exceptions.compute(failureSignature(identifier, throwable)) { _, cachedFailure ->
            cachedFailure?.incrementOccurrences()
                ?: CachedFailure(
                    path = identifier,
                    type = throwable.javaClass.simpleName,
                    message = throwable.message.orEmpty(),
                    trace = trace
                )
        }
    }

    private fun failureSignature(identifier: String, throwable: Throwable): String =
        listOf(
            identifier,
            throwable.javaClass.name,
            throwable.stackTrace.firstOrNull()?.toString() ?: "<unknown stacktrace>"
        ).joinToString(separator = "|")

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
        exceptions.values.sumOf { it.occurrences.get() }

    fun getFailures(): Collection<String> =
        getRecordedFailures().map { it.trace }

    fun getRecordedFailures(): Collection<RecordedFailure> =
        exceptions.values.map { it.toRecordedFailure() }

    override fun getLogger(): Logger =
        journalist.logger

}

private data class CachedFailure(
    val path: String,
    val type: String,
    val message: String,
    val trace: String,
    val occurrences: AtomicInteger = AtomicInteger(1)
) {

    fun incrementOccurrences(): CachedFailure =
        apply { occurrences.incrementAndGet() }

    fun toRecordedFailure(): RecordedFailure =
        RecordedFailure(
            path = path,
            type = type,
            message = message,
            trace = trace,
            occurrences = occurrences.get()
        )

}
