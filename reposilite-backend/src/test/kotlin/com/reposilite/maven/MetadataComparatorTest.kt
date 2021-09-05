package com.reposilite.maven

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MetadataComparatorTest {

    private data class FileInfo(
        val name: String,
        val isDirectory: Boolean
    )

    private val comparator = FilesComparator<FileInfo>(
        { file -> VersionComparator.DEFAULT_VERSION_PATTERN.split(file.name) },
        { it.isDirectory }
    )

    @Test
    fun `should sort versions in ascending order`() {
        // given: an unordered list of files
        val files = listOf(
            FileInfo("Reposilite", false),
            FileInfo("1.0.3", false),
            FileInfo("1.0.3", true),
            FileInfo("1.0.2", true),
            FileInfo("1.0.1", true)
        )

        // when: an unordered list is sorted
        val result = files.sortedWith(comparator)

        // then: sorted list matches expected rules
        assertEquals(
            listOf(
                FileInfo("1.0.1", true),
                FileInfo("1.0.2", true),
                FileInfo("1.0.3", true),
                FileInfo("1.0.3", false),
                FileInfo("Reposilite", false)
            ),
            result
        )
    }

}