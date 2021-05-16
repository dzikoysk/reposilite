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

package org.panda_lang.reposilite.maven.metadata

import groovy.transform.CompileStatic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@CompileStatic
class MetadataTest {

    private static final Versioning VERSIONING = new Versioning()
    private static final Metadata METADATA = new Metadata("group", "artifact", "version", VERSIONING)

    @Test
    void 'should return group '() {
        assertEquals "group", METADATA.getGroupId()
    }

    @Test
    void 'should return artifact'() {
        assertEquals "artifact", METADATA.getArtifactId()
    }

    @Test
    void 'should return version' () {
        assertEquals "version", METADATA.getVersion()
    }

    @Test
    void 'shourt return artifact versioning'() {
        assertEquals VERSIONING, METADATA.getVersioning()
    }

    @Test
    void 'should be empty' () {
        assertNull new Metadata().getGroupId()
        assertNull new Metadata().getArtifactId()
        assertNull new Metadata().getVersion()
        assertNull new Metadata().getVersioning()
    }

}