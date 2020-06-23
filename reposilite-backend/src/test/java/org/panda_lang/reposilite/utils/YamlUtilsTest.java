package org.panda_lang.reposilite.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.reposilite.config.Configuration;

import java.io.File;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertThrows;

class YamlUtilsTest {

    @TempDir
    static File temp;

    @Test
    void forceLoad() {
        assertThrows(RuntimeException.class, () -> YamlUtils.forceLoad(temp, Configuration.class, Function.identity()));
    }

}