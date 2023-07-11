package com.reposilite.configuration

import com.reposilite.configuration.local.infrastructure.LOCAL_CONFIGURATION_FILE
import com.reposilite.configuration.shared.infrastructure.SHARED_CONFIGURATION_FILE
import com.reposilite.createWithParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class ParametersTest {

    @TempDir
    lateinit var workingDirectory: File

    @Test
    fun `should create default local configuration`() {
        createWithParameters("--working-directory=$workingDirectory", "--generate-configuration=local")
        val localConfiguration = workingDirectory.resolve(LOCAL_CONFIGURATION_FILE)
        assertThat(localConfiguration.exists()).isTrue
        assertThat(localConfiguration.readText()).contains("Reposilite :: Local")
    }

    @Test
    fun `should create default shared configuration`() {
        createWithParameters("--working-directory=$workingDirectory", "--generate-configuration=shared")
        val sharedConfiguration = workingDirectory.resolve(SHARED_CONFIGURATION_FILE)
        assertThat(sharedConfiguration.exists()).isTrue
        assertThat(sharedConfiguration.readText()).contains("\"web\"")
    }

}
