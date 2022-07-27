/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.configuration

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.configuration.specification.SharedConfigurationSpecification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SharedConfigurationFacadeTest : SharedConfigurationSpecification() {

    @Doc(title = "Test", description = "Description")
    data class TestSettings(
        @Doc(title = "Property", description = "Sets property to the given value")
        val property: String = "value"
    ) : SharedSettings

    @Test
    fun `should generate a valid json schema describing settings entity`() {
        // given: a known domain settings

        // when: test's schema is requested
        val schema = sharedConfigurationFacade.getSettingsReference<TestSettings>("test")!!.schema

        // then: the response is a valid json schema
        assertEquals(
            """
            {
              "${'$'}schema" : "http://json-schema.org/draft-07/schema#",
              "type" : "object",
              "properties" : {
                "property" : {
                  "type" : "string",
                  "title" : "Property",
                  "description" : "Sets property to the given value"
                }
              },
              "title" : "Test",
              "description" : "Description",
              "additionalProperties" : false
            }
            """.trimIndent().replace("\n", System.lineSeparator()),
            schema.toPrettyString()
        )
    }

}
