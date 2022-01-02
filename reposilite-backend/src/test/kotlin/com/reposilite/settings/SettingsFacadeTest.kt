package com.reposilite.settings

import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.settings.application.SettingsPlugin.Companion.SHARED_CONFIGURATION_FILE
import com.reposilite.settings.specification.SettingsSpecification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk

internal class SettingsFacadeTest : SettingsSpecification() {

    @Test
    fun `should create, load, validate and fetch remote configuration`() {
        // then: configuration is up-to-date in provider & repository
        renderConfiguration().run {
            assertEquals(this, assertOk(configurationFromProvider()))
            assertEquals(this, configurationFromRepository())
        }

        // when: user updates configuration
        val updateResult = settingsFacade.updateConfiguration(SettingsUpdateRequest(SHARED_CONFIGURATION_FILE, "id: custom"))
        // then: configuration should be updated in provider & repository
        assertOk(updateResult)
        assertEquals("custom", settingsFacade.sharedConfiguration.id.get())
        renderConfiguration().run {
            assertEquals(this, assertOk(configurationFromProvider()))
            assertEquals(this, configurationFromRepository())
        }
    }

}