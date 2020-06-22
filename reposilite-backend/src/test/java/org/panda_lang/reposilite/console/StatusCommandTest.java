package org.panda_lang.reposilite.console;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturnTrueAndDisplayStatus() {
        super.reposilite.throwException("/",  new RuntimeException());
        assertTrue(new StatusCommand().execute(super.reposilite));
    }

}