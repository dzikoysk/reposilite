package org.panda_lang.reposilite.console;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturnTrueAndDisplayStatus() {
        StatusCommand statusCommand = new StatusCommand();
        assertTrue(statusCommand.execute(super.reposilite));
    }

}