package org.panda_lang.reposilite.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SnapshotTest {

    private static final String TIMESTAMP = Long.toString(System.currentTimeMillis());
    private static final Snapshot SNAPSHOT = new Snapshot(TIMESTAMP, "1");

    @Test
    void getBuildNumber() {
        assertEquals(TIMESTAMP, SNAPSHOT.getTimestamp());
    }

    @Test
    void getTimestamp() {
        assertEquals("1", SNAPSHOT.getBuildNumber());
    }

    @Test
    void shouldBeEmpty() {
        assertNull(new Snapshot().getTimestamp());
        assertNull(new Snapshot().getBuildNumber());
    }

}