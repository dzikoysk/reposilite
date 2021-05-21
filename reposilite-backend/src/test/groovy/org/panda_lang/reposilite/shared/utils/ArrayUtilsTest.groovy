/*
 * Copyright (c) 2021 dzikoysk
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

package org.panda_lang.reposilite.shared.utils

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.panda_lang.utilities.commons.StringUtils

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull

@CompileStatic
class ArrayUtilsTest {

    private static final String[] ARRAY = [ "a", "b", "c" ]

    @Test
    void 'should return first element from array' () {
        assertNull ArrayUtils.getFirst(StringUtils.EMPTY_ARRAY)
        assertEquals "a", ArrayUtils.getFirst(ARRAY)
    }

    @Test
    void 'should return last element from array' () {
        assertNull ArrayUtils.getLast(StringUtils.EMPTY_ARRAY)
        assertEquals "c", ArrayUtils.getLast(ARRAY)
    }

}