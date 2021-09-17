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

import com.reposilite.status.specification.FailureSpecification
import org.junit.jupiter.api.Assertions.assertTrue
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
        assertTrue(failureFacade.hasFailures())
        assertTrue(failureFacade.getFailures().iterator().next().contains(message))
    }

}