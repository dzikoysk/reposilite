package com.reposilite

import com.reposilite.configuration.local.infrastructure.LOCAL_CONFIGURATION_FILE
import com.reposilite.configuration.shared.infrastructure.SHARED_CONFIGURATION_FILE
import org.junit.jupiter.api.Assertions.assertTrue
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
        assertTrue(localConfiguration.exists())
        assertTrue(localConfiguration.readText().contains("Reposilite :: Local"))
    }

    @Test
    fun `should create default shared configuration`() {
        createWithParameters("--working-directory=$workingDirectory", "--generate-configuration=shared")
        val sharedConfiguration = workingDirectory.resolve(SHARED_CONFIGURATION_FILE)
        assertTrue(sharedConfiguration.exists())
        assertTrue(sharedConfiguration.readText().contains("\"web\""))
    }

}
