package org.panda_lang.reposilite.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void map() {
        assertEquals(7, Result.ok("7").map(Integer::parseInt).getValue());
    }

    @Test
    void orElse() {
        assertEquals(7, Result.error(-1).orElse(err -> Result.ok(7)).getValue());
    }

    @Test
    void orElseGet() {
        assertEquals(7, Result.error(-1).orElseGet(err -> 7));
    }

    @Test
    void onError() {
        AtomicInteger integer = new AtomicInteger(-1);
        Result.error(integer.get()).onError(err -> integer.set(Math.abs(err)));
        assertEquals(1, integer.get());
    }

    @Test
    void isDefined() {
        assertTrue(Result.ok("ok").isDefined());
        assertFalse(Result.error("err").isDefined());
    }

    @Test
    void getValue() {
        assertEquals("value", Result.ok("value").getValue());
    }

    @Test
    void containsError() {
        assertTrue(Result.error("err").containsError());
        assertFalse(Result.ok("ok").containsError());
    }

    @Test
    void getError() {
        assertEquals("err", Result.error("err").getError());
    }

}