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