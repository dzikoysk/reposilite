package org.panda_lang.reposilite.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FilesUtilsTest {

    @Test
    void close() {
        assertDoesNotThrow(() -> FilesUtils.close(null));
        assertDoesNotThrow(() -> FilesUtils.close(new ByteArrayInputStream(new byte[0])));
    }

}