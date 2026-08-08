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

import com.reposilite.status.api.RecordedFailure
import com.reposilite.status.specification.FailureSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class FailureFacadeTest : FailureSpecification() {

    @Test
    fun `should store failure`() {
        // given: an exception with a message
        val message = "Unlucky"
        val exception = failure(message)

        // when: an error has been registered in failure facade
        failureFacade.throwException("PATH /com/reposilite", exception)

        // then: service properly registered thrown exception
        val recordedFailure = RecordedFailure(
            path = "PATH /com/reposilite",
            type = "RuntimeException",
            message = message,
            messages = listOf(message),
            trace = trace(message),
            occurrences = 1
        )
        assertThat(failureFacade.hasFailures()).isTrue
        assertThat(failureFacade.getRecordedFailures()).containsExactly(recordedFailure)
        assertThat(failureFacade.getFailures()).containsExactly(recordedFailure.trace)
    }

    @Test
    fun `should count duplicated failures`() {
        // when: the same source fails more than once
        listOf("Unlucky", "Still unlucky").forEach { message ->
            failureFacade.throwException("PATH /com/reposilite", failure(message))
        }

        // then: the failure is stored once with an incremented occurrence count
        val recordedFailure = RecordedFailure(
            path = "PATH /com/reposilite",
            type = "RuntimeException",
            message = "Unlucky",
            messages = listOf("Unlucky", "Still unlucky"),
            trace = trace("Unlucky"),
            occurrences = 2
        )
        assertThat(failureFacade.getRecordedFailures()).containsExactly(recordedFailure)
        assertThat(failureFacade.getFailures()).containsExactly(recordedFailure.trace)
        assertThat(failureFacade.getFailuresCount()).isEqualTo(2)
    }

    @Test
    fun `should separate failures with different root causes`() {
        // when: the same source and wrapper location fail for different reasons
        failureFacade.throwException(
            "PATH /com/reposilite",
            failure("Wrapped failure", cause(IllegalArgumentException("invalid")))
        )
        failureFacade.throwException(
            "PATH /com/reposilite",
            failure("Wrapped failure", cause(IllegalStateException("unavailable")))
        )

        // then: each root cause is recorded as a separate failure
        assertThat(failureFacade.getRecordedFailures()).containsExactlyInAnyOrder(
            RecordedFailure(
                path = "PATH /com/reposilite",
                type = "RuntimeException",
                message = "Wrapped failure",
                messages = listOf("Wrapped failure"),
                trace = trace("Wrapped failure", "IllegalArgumentException: invalid"),
                occurrences = 1
            ),
            RecordedFailure(
                path = "PATH /com/reposilite",
                type = "RuntimeException",
                message = "Wrapped failure",
                messages = listOf("Wrapped failure"),
                trace = trace("Wrapped failure", "IllegalStateException: unavailable"),
                occurrences = 1
            )
        )
    }

    private fun failure(message: String, cause: Throwable? = null): RuntimeException =
        RuntimeException(message, cause).apply {
            stackTrace = arrayOf(StackTraceElement("FailureFacadeTest", "failure", "FailureFacadeTest.kt", 1))
        }

    private fun <T : Throwable> cause(throwable: T): T =
        throwable.apply {
            stackTrace = arrayOf(StackTraceElement("FailureFacadeTest", "cause", "FailureFacadeTest.kt", 2))
        }

    private fun trace(message: String, cause: String? = null): String =
        listOfNotNull(
            "failure PATH /com/reposilite",
            "  by RuntimeException: $message",
            "  at FailureFacadeTest.failure(FailureFacadeTest.kt:1)",
            cause?.let { "  by $it" },
            cause?.let { "  at FailureFacadeTest.cause(FailureFacadeTest.kt:2)" }
        ).joinToString(System.lineSeparator())

}
