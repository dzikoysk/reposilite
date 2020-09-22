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

package org.panda_lang.reposilite.utils

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import java.util.concurrent.atomic.AtomicInteger

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class ResultTest {

    @Test
    void 'should map map value' () {
        assertEquals 7, Result.ok("7").map({ value -> Integer.parseInt(value) }).getValue()
    }

    @Test
    void 'should return else value on error' () {
        assertEquals 7, Result.error(-1).orElse({ err -> Result.ok(7) }).getValue()
    }

    @Test
    void 'should get else value on error' () {
        assertEquals 7, Result.error(-1).orElseGet({ err -> 7 })
    }

    @Test
    void 'should evaluate error runnable on error' () {
        AtomicInteger integer = new AtomicInteger(-1)
        Result.error(integer.get()).onError({ err -> integer.set(Math.abs(err)) })
        assertEquals 1, integer.get()
    }

    @Test
    void 'should be defined' () {
        assertTrue Result.ok("ok").isDefined()
        assertFalse Result.error("err").isDefined()
    }

    @Test
    void 'should return proper value' () {
        assertEquals "value", Result.ok("value").getValue()
    }

    @Test
    void 'should contain error' () {
        assertTrue Result.error("err").containsError()
        assertFalse Result.ok("ok").containsError()
    }

    @Test
    void 'should return error' () {
        assertEquals "err", Result.error("err").getError()
    }

}