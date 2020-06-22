package org.panda_lang.reposilite.console;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;
import org.panda_lang.utilities.commons.StringUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleTest extends ReposiliteIntegrationTest {

    @Test
    void shouldFalseOnEmpty() {
        assertFalse(super.reposilite.getConsole().execute(StringUtils.EMPTY));
    }

    @Test
    void shouldSucceedOnSingleElement() {
        Console console = super.reposilite.getConsole();
        assertTrue(console.execute("help"));
        assertTrue(console.execute("version"));
        assertTrue(console.execute("status"));
        assertTrue(console.execute("purge"));
        assertTrue(console.execute("tokens"));
        assertTrue(console.execute("gc"));
        assertTrue(console.execute("stop"));
    }

    @Test
    void shouldSucceedOnComplex() {
        Console console = super.reposilite.getConsole();

        assertTrue(console.execute("stats"));
        assertTrue(console.execute("stats 10"));
        assertTrue(console.execute("stats /"));

        assertTrue(console.execute("keygen / root"));
        assertTrue(console.execute("revoke root"));
    }

    @Test
    void shouldFalseOnUnknown() {
        assertFalse(super.reposilite.getConsole().execute("unknown"));
    }

}