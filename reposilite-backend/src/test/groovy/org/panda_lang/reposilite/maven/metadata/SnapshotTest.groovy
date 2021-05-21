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

package org.panda_lang.reposilite.maven.metadata

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull

@CompileStatic
class SnapshotTest {

    private static final String TIMESTAMP = Long.toString(System.currentTimeMillis())
    private static final Snapshot SNAPSHOT = new Snapshot(TIMESTAMP, "1")

    @Test
    void 'should return timestamp' () {
        assertEquals TIMESTAMP, SNAPSHOT.getTimestamp()
    }

    @Test
    void 'should return build number' () {
        assertEquals "1", SNAPSHOT.getBuildNumber()
    }

    @Test
    void 'should be empty' () {
        assertNull new Snapshot().getTimestamp()
        assertNull new Snapshot().getBuildNumber()
    }

}