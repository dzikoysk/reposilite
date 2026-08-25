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

package com.reposilite.plugin.prometheus

import com.reposilite.plugin.prometheus.specification.PrometheusPluginSpecification
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class PrometheusPluginTest : PrometheusPluginSpecification() {

    @Test
    fun `should fetch metrics without failing`() {
        System.setProperty("reposilite.prometheus.user", "user")
        System.setProperty("reposilite.prometheus.password", "password")

        assertDoesNotThrow {
            val facade = prometheusPlugin.initialize()
            val result = facade.getMetrics(null, setOf())
            assert(result.isOk)
        }
    }
}
