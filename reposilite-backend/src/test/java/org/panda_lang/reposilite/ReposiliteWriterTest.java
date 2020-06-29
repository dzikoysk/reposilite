package org.panda_lang.reposilite;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReposiliteWriterTest {

    @Test
    void getConsumers() {
        AtomicReference<String> reference = new AtomicReference<>(null);

        ReposiliteWriter.getConsumers().put("key", reference::set);
        Reposilite.getLogger().info("ReposiliteWriterTest message");

        assertTrue(reference.get().contains("ReposiliteWriterTest message"));
    }

    @Test
    void getLatest() {
        for (int index = 0; index < ReposiliteWriter.getCacheSize(); index++) {
            Reposilite.getLogger().info(Integer.toString(index));
        }

        assertEquals(ReposiliteWriter.getCacheSize(), ReposiliteWriter.getCache().size());
        Reposilite.getLogger().info("above limit");
        assertEquals(ReposiliteWriter.getCacheSize(), ReposiliteWriter.getCache().size());
    }

    @Test
    void contains() {
        Reposilite.getLogger().info("test::contains");
        assertTrue(ReposiliteWriter.contains("test::contains"));
        assertFalse(ReposiliteWriter.contains("diorite::release_date"));
    }

}