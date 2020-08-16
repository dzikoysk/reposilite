package org.panda_lang.reposilite.repository


import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

final class DiskQuotaTest {

    @TempDir
    public File workingDirectory

    @Test
    @SuppressWarnings('GroovyAccessibility')
    void 'should create quota of the given percentage' () {
        def quota = DiskQuota.of(workingDirectory, '90%')
        def size = quota.@quota.longValue()

        assertTrue size > 0
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
