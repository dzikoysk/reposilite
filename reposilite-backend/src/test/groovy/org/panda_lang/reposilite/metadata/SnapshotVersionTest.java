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

package org.panda_lang.reposilite.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SnapshotVersionTest {

    private static final SnapshotVersion SNAPSHOT_VERSION = new SnapshotVersion("pom", "file-1.0.0",  "2020");

    @Test
    void getExtension() {
        assertEquals("pom", SNAPSHOT_VERSION.getExtension());
    }

    @Test
    void getValue() {
        assertEquals("file-1.0.0", SNAPSHOT_VERSION.getValue());
    }

    @Test
    void getUpdated() {
        assertEquals("2020", SNAPSHOT_VERSION.getUpdated());
    }

    @Test
    void shouldBeEmpty() {
        assertNull(new SnapshotVersion().getExtension());
        assertNull(new SnapshotVersion().getValue());
        assertNull(new SnapshotVersion().getUpdated());
    }

}