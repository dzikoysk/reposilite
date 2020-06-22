package org.panda_lang.reposilite.metadata;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersioningTest {

    private static final List<SnapshotVersion> SNAPSHOT_VERSION_LIST = Arrays.asList(
            new SnapshotVersion("pom", "file-1.0.0", "2020"),
            new SnapshotVersion("pom", "file-0.9.9", "2020")
    );

    private static final Snapshot SNAPSHOT = new Snapshot("2020", "1");

    private static final Versioning VERSIONING = new Versioning(
            "1.0.0",
            "1.0.1",
            Arrays.asList("1.0.0", "0.9.9"),
            SNAPSHOT,
            SNAPSHOT_VERSION_LIST,
            "2020"
    );

    @Test
    void getRelease() {
        assertEquals("1.0.0", VERSIONING.getRelease());
    }

    @Test
    void getLatest() {
        assertEquals("1.0.1", VERSIONING.getLatest());
    }

    @Test
    void getVersions() {
        assertEquals(Arrays.asList("1.0.0", "0.9.9"), VERSIONING.getVersions());
    }

    @Test
    void getSnapshot() {
        assertEquals(SNAPSHOT, VERSIONING.getSnapshot());
    }

    @Test
    void getSnapshotVersions() {
        assertEquals(SNAPSHOT_VERSION_LIST, VERSIONING.getSnapshotVersions());
    }

    @Test
    void getLastUpdated() {
        assertEquals("2020", VERSIONING.getLastUpdated());
    }

}