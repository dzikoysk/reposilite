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

package com.reposilite.configuration

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.configuration.specification.SharedReposiliteConfigurationSpecification
import io.javalin.openapi.JsonSchema
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.SECONDS

internal class SharedReposiliteConfigurationFacadeTest : SharedReposiliteConfigurationSpecification() {

    @JsonSchema(requireNonNulls = false)
    @Doc(title = "Test", description = "Description")
    data class TestSettings(
        @get:Doc(title = "Property", description = "Sets property to the given value")
        val property: String = "value"
    ) : SharedSettings

    @JsonSchema(requireNonNulls = false)
    @Doc(title = "Other", description = "Another settings domain")
    data class OtherSettings(val property: String = "value") : SharedSettings

    override val settings = listOf(TestSettings(), OtherSettings())

    @Test
    fun `should recompute dependencies without persisting direct settings updates`() {
        // given: a dependency computed from settings
        val settings = sharedConfigurationFacade.getDomainSettings<TestSettings>()
        val property = settings.computed { it.property }

        // when: settings are updated directly
        settings.update { it.copy(property = "updated") }

        // then: dependencies have recomputed before the update returns, without persistence
        assertThat(property.get()).isEqualTo("updated")
        assertThat(configurationProvider.content).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should finish notifying one domain before updating another`(useFunction: Boolean) {
        // given: one domain whose dependent work has not finished
        val settings = sharedConfigurationFacade.getDomainSettings<TestSettings>()
        val other = sharedConfigurationFacade.getDomainSettings<OtherSettings>()

        // when: another domain is updated while that work is running
        useConcurrentUpdates(
            first = { pause ->
                settings.computed { if (it.property == "first") pause() }
                settings.update(TestSettings("first"))
            },
            second = {
                when (useFunction) {
                    true -> other.update { it.copy(property = "second") }
                    false -> other.update(OtherSettings("second"))
                }
            },
        )

        // then: both updates complete in order
        assertThat(settings.get().property).isEqualTo("first")
        assertThat(other.get().property).isEqualTo("second")
    }

    @Test
    fun `should compute an update from the latest settings`() {
        // given: settings with an update still computing its value
        val settings = sharedConfigurationFacade.getDomainSettings<TestSettings>()

        // when: a second update reads and changes the same settings
        useConcurrentUpdates(
            first = { pause ->
                settings.update {
                    pause()
                    it.copy(property = "first")
                }
            },
            second = { settings.update { it.copy(property = "${it.property}-second") } },
        )

        // then: the second update observes the completed first update
        assertThat(settings.get().property).isEqualTo("first-second")
    }

    @Test
    fun `should serialize named reference updates with direct updates`() {
        // given: both ways of accessing the same domain
        val settings = sharedConfigurationFacade.getDomainSettings<TestSettings>()
        val named = sharedConfigurationFacade.getSettingsReference<TestSettings>("test")!!

        // when: the named reference is updated while a direct update is in progress
        useConcurrentUpdates(
            first = { pause ->
                settings.update {
                    pause()
                    TestSettings("first")
                }
            },
            second = { assertThat(named.update(TestSettings("second")).get().property).isEqualTo("second") },
        )

        // then: both references expose the final update
        assertThat(settings.get().property).isEqualTo("second")
        assertThat(named.get()).isEqualTo(settings.get())
    }

    @Test
    fun `should finish persisting settings before accepting another update`() {
        // given: an update that pauses during persistence
        val settings = sharedConfigurationFacade.getDomainSettings<TestSettings>()

        // when: a direct update arrives while the first update is being saved
        useConcurrentUpdates(
            first = { pause ->
                configurationProvider.onUpdate = pause
                sharedConfigurationFacade.updateSharedSettings("test", TestSettings("persisted"))
            },
            second = { settings.update(TestSettings("in-memory")) },
        )

        // then: persistence completes first and the direct update remains in memory
        assertThat(configurationProvider.content).contains("\"property\":\"persisted\"")
        assertThat(settings.get().property).isEqualTo("in-memory")
    }

    @Test
    fun `should apply fetched configuration before accepting a local settings update`() {
        // given: remote settings waiting to be synchronized
        configurationProvider.content = """{"test":{"property":"remote"}}"""
        configurationProvider.updateRequired = true

        // when: a local update arrives while the remote configuration is being fetched
        useConcurrentUpdates(
            first = { pause ->
                configurationProvider.onFetch = pause
                sharedConfigurationFacade.synchronize()
            },
            second = { sharedConfigurationFacade.updateSharedSettings("test", TestSettings("local")) },
        )

        // then: the local update is not overwritten by the earlier remote configuration
        assertThat(sharedConfigurationFacade.getDomainSettings<TestSettings>().get().property).isEqualTo("local")
        assertThat(configurationProvider.content).contains("\"property\":\"local\"")
    }

    @Test
    fun `should not fetch unchanged configuration`() {
        // given: configuration without a pending remote update
        configurationProvider.onFetch = { error("Unchanged configuration should not be fetched") }

        // when: synchronization runs
        sharedConfigurationFacade.synchronize()

        // then: current settings are preserved
        assertThat(sharedConfigurationFacade.getDomainSettings<TestSettings>().get()).isEqualTo(TestSettings())
    }

    @Test
    fun `should finish saving local settings before checking for remote changes`() {
        // given: remote configuration superseded by a pending local save
        configurationProvider.content = """{"test":{"property":"remote"}}"""
        configurationProvider.updateRequired = true
        configurationProvider.onFetch = { error("The completed local save should not be fetched again") }

        // when: synchronization runs while the local update is being saved
        useConcurrentUpdates(
            first = { pause ->
                configurationProvider.onUpdate = pause
                sharedConfigurationFacade.updateSharedSettings("test", TestSettings("local"))
            },
            second = { sharedConfigurationFacade.synchronize() },
        )

        // then: synchronization observes the completed save and leaves local settings intact
        assertThat(sharedConfigurationFacade.getDomainSettings<TestSettings>().get().property).isEqualTo("local")
        assertThat(configurationProvider.content).contains("\"property\":\"local\"")
    }

    @Test
    fun `should allow settings updates after a subscriber fails`() {
        // given: a subscriber that rejects a particular update
        val settings = sharedConfigurationFacade.getDomainSettings<TestSettings>()
        settings.subscribe { check(it.property != "invalid") }

        // when: an update fails and is followed by a valid one
        assertThatThrownBy { settings.update(TestSettings("invalid")) }
            .isInstanceOf(IllegalStateException::class.java)
        CompletableFuture.runAsync { settings.update(TestSettings("valid")) }.get(5, SECONDS)

        // then: the failed subscriber does not prevent later updates
        assertThat(settings.get().property).isEqualTo("valid")
    }

    @Test
    fun `should generate a valid json schema describing settings entity`() {
        // given: a known domain settings

        // when: test's schema is requested
        val schema = sharedConfigurationFacade.getSettingsReference<TestSettings>("test")!!.schema.get().reader().readText()

        // then: the response is a valid json schema
        assertThat(schema).isEqualTo(
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
            """.trimIndent().replace("\n", System.lineSeparator())
        )
    }

}
