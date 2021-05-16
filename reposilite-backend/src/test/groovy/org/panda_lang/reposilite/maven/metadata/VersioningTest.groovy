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

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class VersioningTest {

    private static final List<SnapshotVersion> SNAPSHOT_VERSION_LIST = Arrays.asList(
            new SnapshotVersion("pom", "file-1.0.0", "2020"),
            new SnapshotVersion("pom", "file-0.9.9", "2020")
    )

    private static final Snapshot SNAPSHOT = new Snapshot("2020", "1")

    private static final Versioning VERSIONING = new Versioning(
            "1.0.0",
            "1.0.1",
            Arrays.asList("1.0.0", "0.9.9"),
            SNAPSHOT,
            SNAPSHOT_VERSION_LIST,
            "2020"
    )

    @Test
    void 'should return release identifier' () {
        assertEquals "1.0.0", VERSIONING.getRelease()
    }

    @Test
    void 'should return latest identifier' () {
        assertEquals "1.0.1", VERSIONING.getLatest()
    }

    @Test
    void 'should return version identifier' () {
        assertEquals Arrays.asList("1.0.0", "0.9.9"), VERSIONING.getVersions()
    }

    @Test
    void 'should return snapshot' () {
        assertEquals SNAPSHOT, VERSIONING.getSnapshot()
    }

    @Test
    void 'should return snapshot versions' () {
        assertEquals SNAPSHOT_VERSION_LIST, VERSIONING.getSnapshotVersions()
    }

    @Test
    void 'should return last updated timestamp' () {
        assertEquals "2020", VERSIONING.getLastUpdated()
    }

}