/*
 * Copyright (c) 2026 dzikoysk
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

package com.reposilite.web

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class HttpServerTest {

    @Test
    fun `should require enough web threads for configured connectors`() {
        assertThatCode { validateWebThreadPoolSize(maxThreads = 4, sslEnabled = false) }
            .doesNotThrowAnyException()
        assertThatCode { validateWebThreadPoolSize(maxThreads = 6, sslEnabled = true) }
            .doesNotThrowAnyException()

        assertThatThrownBy { validateWebThreadPoolSize(maxThreads = 3, sslEnabled = false) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Web thread pool size must be at least 4")
        assertThatThrownBy { validateWebThreadPoolSize(maxThreads = 5, sslEnabled = true) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Web thread pool size must be at least 6 when SSL is enabled")
    }

}
