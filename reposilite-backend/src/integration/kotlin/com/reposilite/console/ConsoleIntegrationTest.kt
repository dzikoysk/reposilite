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

package com.reposilite.console

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.console.specification.ConsoleIntegrationSpecification
import com.reposilite.status.FailureFacade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class ConsoleIntegrationTest : ConsoleIntegrationSpecification() {

    @Test
    fun `should close consoles before publishing shutdown logs`() {
        // given: two authenticated consoles
        val consoles = listOf(useConsole(), useConsole())

        // when: a log is published after the HTTP server stops
        reposilite.webServer.stop()
        useLog("message after shutdown")

        // then: both streams end and no stale request is resumed
        consoles.forEach { it.readText() }
        assertThat(useFacade<FailureFacade>().getRecordedFailures()).isEmpty()
    }

}
