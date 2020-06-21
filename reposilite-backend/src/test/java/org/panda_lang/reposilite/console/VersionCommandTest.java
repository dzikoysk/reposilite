package org.panda_lang.reposilite.console;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturnTrueAndDisplayVersion() {
        VersionCommand versionCommand = new VersionCommand();
        assertTrue(versionCommand.execute(super.reposilite));
    }

}