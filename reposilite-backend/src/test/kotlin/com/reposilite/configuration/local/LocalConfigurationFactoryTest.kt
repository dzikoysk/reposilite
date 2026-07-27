package com.reposilite.configuration.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LocalConfigurationFactoryTest {

    @Test
    fun `should apply boolean external property`() {
        // given: a default local configuration
        val localConfiguration = LocalConfiguration()

        // when: a boolean external property is applied
        applyProperty(localConfiguration, "DEBUGENABLED", "true")

        // then: the property is updated
        assertThat(localConfiguration.debugEnabled.get()).isTrue
    }

    @Test
    fun `should apply int external property`() {
        // given: a default local configuration
        val localConfiguration = LocalConfiguration()

        // when: an int external property is applied
        applyProperty(localConfiguration, "PORT", "8181")

        // then: the property is updated
        assertThat(localConfiguration.port.get()).isEqualTo(8181)
    }

    @Test
    fun `should apply long external property`() {
        // given: a default local configuration
        val localConfiguration = LocalConfiguration()

        // when: a long external property above the int range is applied
        applyProperty(localConfiguration, "IDLETIMEOUT", "2147483648")

        // then: the property is updated without losing precision
        assertThat(localConfiguration.idleTimeout.get()).isEqualTo(2_147_483_648L)
    }

    @Test
    fun `should apply string external property`() {
        // given: a default local configuration
        val localConfiguration = LocalConfiguration()

        // when: a string external property is applied
        applyProperty(localConfiguration, "HOSTNAME", "reposilite.local")

        // then: the property is updated
        assertThat(localConfiguration.hostname.get()).isEqualTo("reposilite.local")
    }

    private fun applyProperty(localConfiguration: LocalConfiguration, key: String, value: String) {
        LocalConfigurationFactory.applyCustomProperties(
            journalist = null,
            localConfiguration = localConfiguration,
            properties = mapOf(key to value)
        )
    }

}
