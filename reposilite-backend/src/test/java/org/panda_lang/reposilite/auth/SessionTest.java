package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.config.Configuration;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    private static final Configuration DEFAULT_CONFIGURATION = new Configuration();

    static {
        DEFAULT_CONFIGURATION.setRepositories(Arrays.asList("releases", "snapshots"));
    }

    @Test
    void hasPermission() {
        Configuration configuration = new Configuration();
        configuration.setRepositories(Collections.emptyList());
        Session standardSession = new Session(configuration, new Token("/a/b/c", "alias", "token"));

        assertTrue(standardSession.hasPermission("/a/b/c"));
        assertTrue(standardSession.hasPermission("/a/b/c/d"));

        assertFalse(standardSession.hasPermission("/a/b/"));
        assertFalse(standardSession.hasPermission("/a/b/d"));
    }

    @Test
    void hasPermissionWithWildcard() {
        Configuration configuration = new Configuration();
        configuration.setRepositories(Arrays.asList("a", "b"));
        Session wildcardSession = new Session(configuration, new Token("*/b/c", "alias", "token"));

        assertTrue(wildcardSession.hasPermission("/a/b/c"));
        assertTrue(wildcardSession.hasPermission("/a/b/c/d"));
        assertTrue(wildcardSession.hasPermission("/b/b/c"));

        assertFalse(wildcardSession.hasPermission("/a/b"));
        assertFalse(wildcardSession.hasPermission("/b/b"));
        assertFalse(wildcardSession.hasPermission("/x/b/c"));
    }

    @Test
    void hasRootPermission() {
        Session standardRootSession = new Session(DEFAULT_CONFIGURATION, new Token("/", "alias", "token"));
        Session wildcardRootSession = new Session(DEFAULT_CONFIGURATION, new Token("*", "alias", "token"));

        assertTrue(standardRootSession.hasPermission("/"));
        assertFalse(wildcardRootSession.hasPermission("/"));
        assertTrue(wildcardRootSession.hasPermission("/releases"));
        assertTrue(wildcardRootSession.hasPermission("/snapshots"));
    }

    @Test
    void shouldContainRepositoryFromPath() {
        Session session = new Session(DEFAULT_CONFIGURATION, new Token("/releases", "alias", "token"));
        assertEquals(Collections.singletonList("releases"), session.getRepositories());
    }

    @Test
    void shouldReturnEmptyListForUnknownRepositoryInPath() {
        Session session = new Session(DEFAULT_CONFIGURATION, new Token("/unknown_repository", "alias", "token"));
        assertEquals(Collections.emptyList(), session.getRepositories());
    }

}