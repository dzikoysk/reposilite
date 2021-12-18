/*
 * Copyright (c) 2021 dzikoysk
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

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import panda.utilities.StringUtils
import java.util.concurrent.ConcurrentHashMap

class FailureFacade internal constructor(private val journalist: Journalist) : Journalist, Facade {

    private val exceptions = ConcurrentHashMap.newKeySet<String>()

    fun throwException(id: String, throwable: Throwable) {
        logger.error(id)
        logger.exception(throwable)

        exceptions.add(
            arrayOf(
                "failure $id",
                throwException(throwable)
            )
            .joinToString(separator = System.lineSeparator())
            .trim()
        )
    }

    private fun throwException(throwable: Throwable?): String {
        if (throwable == null) {
            return StringUtils.EMPTY
        }

        return arrayOf(
            "  by ${throwable.javaClass.simpleName}: ${throwable.message}",
            "  at " + (throwable.stackTrace.getOrNull(0) ?: "<unknown stacktrace>"),
            throwException(throwable.cause)
        ).joinToString(separator = System.lineSeparator())
    }

    fun hasFailures() =
        exceptions.isNotEmpty()

    fun getFailures(): Collection<String> =
        exceptions

    override fun getLogger(): Logger =
        journalist.logger

}
