package org.panda_lang.reposilite.repository


import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class DiskQuotaTest {

    @TempDir
    public File workingDirectory

    @Test
    @SuppressWarnings('GroovyAccessibility')
    void 'should create quota of the given percentage' () {
        def size = Math.round(workingDirectory.getUsableSpace() * 0.9)
        def quota = DiskQuota.of(workingDirectory, '90%')

        assertEquals size, quota.@quota.longValue()
        assertEquals 0, quota.@usage.longValue()
        assertTrue quota.hasUsableSpace()

        quota.allocate(1)
        assertTrue quota.hasUsableSpace()

        quota.allocate(size)
        assertFalse quota.hasUsableSpace()
    }

    @Test
    @SuppressWarnings('GroovyAccessibility')
    void 'should create quota of the given size' () {
        def size = 10L * 1024 * 1024 * 1024
        def quota = DiskQuota.of(workingDirectory, '10GB')

        assertEquals size, quota.@quota.longValue()
        assertEquals 0, quota.@usage.longValue()
        assertTrue quota.hasUsableSpace()

        quota.allocate(1)
        assertTrue quota.hasUsableSpace()

        quota.allocate(size)
        assertFalse quota.hasUsableSpace()
    }

}
