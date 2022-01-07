package com.reposilite.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError

class LocationTest {

    @Test
    fun `should properly append paths`() {
        assertEquals("group/artifact", Location.of("group").resolve("artifact").toString())
        assertEquals("group/artifact", Location.of("/group").resolve("/artifact").toString())
        assertEquals("group/artifact", Location.of("/////group/////").resolve("/////artifact/////").toString())
        assertEquals("group/artifact", Location.of("\\\\\\group\\\\\\").resolve("\\\\\\artifact\\\\\\").toString())
        assertEquals("시험/기준", Location.of("시험").resolve("기준").toString())
    }

    @Test
    fun `should drop corrupted paths`() {
        assertError(Location.of("../artifact").toPath())
        assertError(Location.of("C:/artifact").toPath())
    }

}