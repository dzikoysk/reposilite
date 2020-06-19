package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.config.Configuration;

import java.util.Arrays;
import java.util.Collections;

class SessionTest {

    @Test
    void hasPermission() {
        Configuration configuration = new Configuration();
        configuration.setRepositories(Collections.emptyList());
        Session standardSession = new Session(configuration, new Token("/a/b/c", "alias", "token"));

        Assertions.assertTrue(standardSession.hasPermission("/a/b/c"));
        Assertions.assertTrue(standardSession.hasPermission("/a/b/c/d"));

        Assertions.assertFalse(standardSession.hasPermission("/a/b/"));
        Assertions.assertFalse(standardSession.hasPermission("/a/b/d"));
    }

    @Test
    void hasPermissionWithWildcard() {
        Configuration configuration = new Configuration();
        configuration.setRepositories(Arrays.asList("a", "b"));
        Session wildcardSession = new Session(configuration, new Token("*/b/c", "alias", "token"));

        Assertions.assertTrue(wildcardSession.hasPermission("/a/b/c"));
        Assertions.assertTrue(wildcardSession.hasPermission("/a/b/c/d"));
        Assertions.assertTrue(wildcardSession.hasPermission("/b/b/c"));

        Assertions.assertFalse(wildcardSession.hasPermission("/a/b"));
        Assertions.assertFalse(wildcardSession.hasPermission("/b/b"));
        Assertions.assertFalse(wildcardSession.hasPermission("/x/b/c"));
    }

    @Test
    void hasRootPermission() {
        Configuration configuration = new Configuration();
        configuration.setRepositories(Arrays.asList("releases", "snapshots"));
        Session standardRootSession = new Session(configuration, new Token("/", "alias", "token"));
        Session wildcardRootSession = new Session(configuration, new Token("*", "alias", "token"));

        Assertions.assertTrue(standardRootSession.hasPermission("/"));
        Assertions.assertFalse(wildcardRootSession.hasPermission("/"));
        Assertions.assertTrue(wildcardRootSession.hasPermission("/releases"));
        Assertions.assertTrue(wildcardRootSession.hasPermission("/snapshots"));
    }

}