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

package com.reposilite.status.specification

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.status.FailureFacade

internal abstract class FailureSpecification {

    protected val failureFacade = FailureFacade(InMemoryLogger())

    protected fun failure(message: String, cause: Throwable? = null): RuntimeException =
        RuntimeException(message, cause).apply {
            stackTrace = arrayOf(StackTraceElement("FailureSpecification", "failure", "FailureSpecification.kt", 1))
        }

    protected fun <T : Throwable> cause(throwable: T): T =
        throwable.apply {
            stackTrace = arrayOf(StackTraceElement("FailureSpecification", "cause", "FailureSpecification.kt", 2))
        }

    protected fun trace(message: String, cause: String? = null): String =
        listOfNotNull(
            "failure PATH /com/reposilite",
            "  by RuntimeException: $message",
            "  at FailureSpecification.failure(FailureSpecification.kt:1)",
            cause?.let { "  by $it" },
            cause?.let { "  at FailureSpecification.cause(FailureSpecification.kt:2)" }
        ).joinToString(System.lineSeparator())

}
