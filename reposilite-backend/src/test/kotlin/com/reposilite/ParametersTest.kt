package com.reposilite

import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.configuration.shared.infrastructure.SHARED_CONFIGURATION_FILE
import com.reposilite.frontend.application.FrontendSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

internal class ParametersTest {

    @TempDir
    lateinit var workingDirectory: File

    @Test
    fun `should load custom shared configuration`() {
        // given: an existing shared configuration to use
        val sharedConfigurationPath = workingDirectory.resolve(SHARED_CONFIGURATION_FILE).toPath()
        Files.writeString(sharedConfigurationPath, """
        {
            "frontend": {
                "id": "test-repository"
            }
        }
        """.trimIndent())

        // when: Reposilite instance is created with custom shared configuration
        val reposilite = createWithParameters("--shared-configuration=$sharedConfigurationPath")!!

        // then: Reposilite should use file-based shared configuration
        val sharedConfigurationFacade = reposilite.extensions.facade<SharedConfigurationFacade>()
        val frontendSettings = sharedConfigurationFacade.getDomainSettings<FrontendSettings>()
        assertEquals("test-repository", frontendSettings.map { it.id })
    }

}