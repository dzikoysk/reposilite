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