package org.panda_lang.reposilite;

import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        Queue<String> queue = ReposiliteWriter.getCache();

        for (int index = 0; index < ReposiliteWriter.CACHE_SIZE; index++) {
            queue.add(Integer.toString(index));
        }

        assertEquals(ReposiliteWriter.CACHE_SIZE, queue.size());
        queue.add("above limit");
        assertEquals(ReposiliteWriter.CACHE_SIZE, queue.size());
    }

}