package org.panda_lang.reposilite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReposiliteLauncherTest {

    @TempDir
    File workingDirectory;

    @Test
    void shouldPrintVersion() {
        ReposiliteLauncher.create("--version");
        assertTrue(ReposiliteWriter.contains(ReposiliteConstants.VERSION));
    }

    @Test
    void shouldPrintHelp() {
        ReposiliteLauncher.create("--help");
        assertTrue(ReposiliteWriter.contains("Commands"));
    }

    @Test
    void shouldReturnReposilite() {
        assertTrue(ReposiliteLauncher.create("-wd=" + workingDirectory.getAbsolutePath()).isPresent());
    }

    @Test
    void shouldLaunchReposilite() {
        InputStream in = new ByteArrayInputStream("stop".getBytes());
        System.setIn(in);

        try {
            System.setProperty("reposilite.debugEnabled", "true");
            System.setProperty("reposilite.port", ReposiliteIntegrationTest.testPort);
            ReposiliteLauncher.main("-wd=" + workingDirectory.getAbsolutePath());
        }
        finally {
            System.clearProperty("reposilite.debugEnabled");
            System.clearProperty("reposilite.port");
        }

        assertTrue(ReposiliteWriter.contains("Debug enabled"));
        assertTrue(ReposiliteWriter.contains("Done"));
    }

}