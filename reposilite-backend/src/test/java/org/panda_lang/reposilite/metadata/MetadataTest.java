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

class MetadataTest {

    private static final Versioning VERSIONING = new Versioning();
    private static final Metadata METADATA = new Metadata("group", "artifact", "version", VERSIONING);

    @Test
    void getGroupId() {
        assertEquals("group", METADATA.getGroupId());
    }

    @Test
    void getArtifactId() {
        assertEquals("artifact", METADATA.getArtifactId());
    }

    @Test
    void getVersion() {
        assertEquals("version", METADATA.getVersion());
    }

    @Test
    void getVersioning() {
        assertEquals(VERSIONING, METADATA.getVersioning());
    }

    @Test
    void shouldBeEmpty() {
        assertNull(new Metadata().getGroupId());
        assertNull(new Metadata().getArtifactId());
        assertNull(new Metadata().getVersion());
        assertNull(new Metadata().getVersioning());
    }

}