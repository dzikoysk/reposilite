package com.reposilite

import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.specification.LocalSpecificationJunitExtension
import com.reposilite.specification.ReposiliteSpecification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files

@ExtendWith(LocalSpecificationJunitExtension::class)
internal class ReposiliteParametersIntegrationTest : ReposiliteSpecification() {

    override fun overrideParameters(parameters: ReposiliteParameters) {
        // given: a custom shared configuration
        parameters.sharedConfigurationPath = reposiliteWorkingDirectory.resolve("custom-shared-configuration.json").toPath()
        Files.writeString(
            parameters.sharedConfigurationPath,
            """
            {
                "frontend": {
                    "id": "test-repository"
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `should load custom shared configuration`() {
        // when: modified settings are requested
        val sharedConfigurationFacade = reposilite.extensions.facade<SharedConfigurationFacade>()
        val frontendSettings = sharedConfigurationFacade.getDomainSettings<FrontendSettings>()

        // then: Reposilite should use file-based shared configuration
        assertEquals("test-repository", frontendSettings.map { it.id })
    }

}