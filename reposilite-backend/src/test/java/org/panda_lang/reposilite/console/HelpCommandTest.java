package org.panda_lang.reposilite.console;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturnTrueAndDisplayMessage() {
        HelpCommand helpCommand = new HelpCommand();
        assertTrue(helpCommand.execute(super.reposilite));
    }

}