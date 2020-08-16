package org.panda_lang.reposilite.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow
import static org.junit.jupiter.api.Assertions.assertEquals

final class FilesUtilsTest {

    @Test
    void 'should convert display size to bytes count' () {
        assertEquals 1024, FilesUtils.displaySizeToBytesCount("1024")
        assertEquals 1024, FilesUtils.displaySizeToBytesCount("1kb")
        assertEquals 1024 * 1024, FilesUtils.displaySizeToBytesCount("1mb")
        assertEquals 1024 * 1024 * 1024, FilesUtils.displaySizeToBytesCount("1gb")
    }

    @Test
    void 'should close closable' () {
        assertDoesNotThrow({ FilesUtils.close(null) } as Executable)
        assertDoesNotThrow({ FilesUtils.close(new ByteArrayInputStream(new byte[0])) } as Executable)

        assertDoesNotThrow({
            def input = new ByteArrayInputStream(new byte[0])
            input.close()
            FilesUtils.close(input)
        } as Executable)
    }

}
