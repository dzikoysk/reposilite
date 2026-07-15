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

import com.reposilite.status.specification.FailureSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class FailureFacadeTest : FailureSpecification() {

    @Test
    fun `should store failure`() {
        // given: an exception with a message
        val message = "Unlucky"
        val exception = RuntimeException(message)

        // when: an error has been registered in failure facade
        failureFacade.throwException("PATH /com/reposilite", exception)

        // then: service properly registered thrown exception
        val recordedFailure = failureFacade.getRecordedFailures().single()
        assertThat(failureFacade.hasFailures()).isTrue
        assertThat(recordedFailure.path).isEqualTo("PATH /com/reposilite")
        assertThat(recordedFailure.type).isEqualTo("RuntimeException")
        assertThat(recordedFailure.message).isEqualTo(message)
        assertThat(recordedFailure.messages).containsExactly(message)
        assertThat(recordedFailure.trace).contains(message)
        assertThat(failureFacade.getFailures().iterator().next()).isEqualTo(recordedFailure.trace)
    }

    @Test
    fun `should count duplicated failures`() {
        // when: the same source fails more than once
        failureFacade.throwException("PATH /com/reposilite", duplicatedFailure("Unlucky"))
        failureFacade.throwException("PATH /com/reposilite", duplicatedFailure("Still unlucky"))

        // then: the failure is stored once with an incremented occurrence count
        val recordedFailure = failureFacade.getRecordedFailures().single()
        assertThat(recordedFailure.occurrences).isEqualTo(2)
        assertThat(recordedFailure.messages).containsExactly("Unlucky", "Still unlucky")
        assertThat(failureFacade.getFailures()).hasSize(1)
        assertThat(failureFacade.getFailuresCount()).isEqualTo(2)
    }

    private fun duplicatedFailure(message: String): RuntimeException =
        RuntimeException(message)

}
