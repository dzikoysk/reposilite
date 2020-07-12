/*
 * Copyright (c) 2020 Dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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