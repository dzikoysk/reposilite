package org.panda_lang.reposilite.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactTest {

    @TempDir
    static File temp;
    static Repository repository;
    static Artifact artifact;

    @BeforeAll
    static void prepare() throws IOException {
        repository = new Repository(temp, "releases");

        File build1 = repository.getFile("groupId", "artifactId", "version", "build1");
        build1.getParentFile().mkdirs();
        build1.createNewFile();

        File build2 = repository.getFile("groupId", "artifactId", "version", "build2");
        build2.getParentFile().mkdirs();
        build2.createNewFile();

        artifact = new Artifact(repository, "groupId", "artifactId", "version");
    }

    @Test
    void getFile() {
        String fileName = ContentJoiner
                .on(File.separator)
                .join(artifact.getRepository().getName(), artifact.getGroup(), artifact.getArtifact(), artifact.getVersion())
                .toString();

        assertEquals(new File(temp, fileName), artifact.getFile(""));
    }

    @Test
    void getLocalPath() {
        assertEquals("groupId/artifactId/version/", artifact.getLocalPath());
    }

    @Test
    void getVersion() {
        assertEquals("version", artifact.getVersion());
    }

    @Test
    void getArtifact() {
        assertEquals("artifactId", artifact.getArtifact());
    }

    @Test
    void getGroup() {
        assertEquals("groupId", artifact.getGroup());
    }

    @Test
    void getRepository() {
        assertEquals("releases", artifact.getRepository().getName());
    }

}