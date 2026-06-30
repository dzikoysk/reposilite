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

package com.reposilite.frontend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BasePathFormatterTest {

    @Test
    fun `should normalize a valid forwarded prefix to a base path`() {
        assertThat(BasePathFormatter.formatForwardedBasePath("/maven")).isEqualTo("/maven/")
        assertThat(BasePathFormatter.formatForwardedBasePath("maven")).isEqualTo("/maven/")
        assertThat(BasePathFormatter.formatForwardedBasePath("/repo/maven/")).isEqualTo("/repo/maven/")
    }

    @Test
    fun `should reject an unsafe forwarded prefix`() {
        // would break out of the HTML attribute
        assertThat(BasePathFormatter.formatForwardedBasePath("/maven'><script>")).isNull()
        assertThat(BasePathFormatter.formatForwardedBasePath("/maven\"")).isNull()
        assertThat(BasePathFormatter.formatForwardedBasePath("/path with spaces")).isNull()
        // would turn the base path into an absolute URL pointing somewhere else
        assertThat(BasePathFormatter.formatForwardedBasePath("//evil.com")).isNull()
    }

}
