package com.reposilite.plugin.mavenindexer

import com.reposilite.plugin.mavenindexer.specification.MavenIndexerPluginSpecification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MavenIndexerPluginTest : MavenIndexerPluginSpecification() {

    @Test
    fun `math should work`() {
        assertEquals(4, 2 + 2, "someone broke math")
    }

}