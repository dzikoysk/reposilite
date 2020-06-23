package org.panda_lang.reposilite.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeUtilsTest {

    @Test
    void format() {
        assertEquals("3.14", TimeUtils.format(Math.PI));
    }

    @Test
    void getUptime() {
        assertTrue(TimeUtils.getUptime(System.currentTimeMillis()) < 1);
    }

}