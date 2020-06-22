package org.panda_lang.reposilite.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RepositoryTest {

    @TempDir
    static File temp;
    static Repository repository;

    @BeforeAll
    static void prepare() throws IOException {
        repository = new Repository(temp, "releases");
        repository.getFile("group", "artifact", "version").mkdirs();
        repository.getFile("group", "artifact", "version", "test").createNewFile();
    }

    @Test
    void get() {
        assertNull(repository.get("unknown"));
        assertEquals("test", Objects.requireNonNull(repository.get("group", "artifact", "version", "test")).getFile("test").getName());
    }

    @Test
    void getFile() {
        assertEquals("test", repository.getFile("test").getName());
    }

    @Test
    void getName() {
        assertEquals("releases", repository.getName());
    }

}