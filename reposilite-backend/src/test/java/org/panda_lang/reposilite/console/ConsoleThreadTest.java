package org.panda_lang.reposilite.console;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.ReposiliteWriter;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleThreadTest {

    @Test
    void shouldPrintVersionMessage() {
        executeInput("version");

        assertTrue(ContentJoiner
                .on("")
                .join(ReposiliteWriter.getCache())
                .toString()
                .contains(ReposiliteConstants.VERSION));
    }

    @Test
    void shouldPrintDockerInfo() {
        executeInput("");

        assertTrue(ContentJoiner
                .on("")
                .join(ReposiliteWriter.getCache())
                .toString()
                .contains("Docker"));
    }

    @SuppressWarnings("CallToThreadRun")
    private void executeInput(String command) {
        ByteArrayInputStream in = new ByteArrayInputStream(command.getBytes());
        Console console = new Console(null, in);
        ConsoleThread thread = new ConsoleThread(console, in);
        thread.run();
    }

}