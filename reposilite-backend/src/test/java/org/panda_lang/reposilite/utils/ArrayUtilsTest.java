package org.panda_lang.reposilite.utils;

import org.junit.jupiter.api.Test;
import org.panda_lang.utilities.commons.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArrayUtilsTest {

    private static final String[] EMPTY = StringUtils.EMPTY_ARRAY;
    private static final String[] ARRAY = { "a", "b", "c" };

    @Test
    void getLatest() {
        assertNull(ArrayUtils.getFirst(EMPTY));
        assertEquals("a", ArrayUtils.getFirst(ARRAY));
    }

    @Test
    void getLast() {
        assertNull(ArrayUtils.getLast(EMPTY));
        assertEquals("c", ArrayUtils.getLast(ARRAY));
    }

}