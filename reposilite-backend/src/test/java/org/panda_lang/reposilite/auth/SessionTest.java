package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class SessionTest {

    @Test
    void hasPermission() {
        Session standardSession = new Session(new Token("/a/b/c", "alias", "token"));

        Assertions.assertTrue(standardSession.hasPermission(Collections.emptyList(), "/a/b/c"));
        Assertions.assertTrue(standardSession.hasPermission(Collections.emptyList(), "/a/b/c/d"));

        Assertions.assertFalse(standardSession.hasPermission(Collections.emptyList(), "/a/b/"));
        Assertions.assertFalse(standardSession.hasPermission(Collections.emptyList(), "/a/b/d"));
    }

    @Test
    void hasPermissionWithWildcard() {
        List<String> repositories = Arrays.asList("a", "b");
        Session wildcardSession = new Session(new Token("*/b/c", "alias", "token"));

        Assertions.assertTrue(wildcardSession.hasPermission(repositories, "/a/b/c"));
        Assertions.assertTrue(wildcardSession.hasPermission(repositories, "/a/b/c/d"));
        Assertions.assertTrue(wildcardSession.hasPermission(repositories, "/b/b/c"));

        Assertions.assertFalse(wildcardSession.hasPermission(repositories, "/a/b"));
        Assertions.assertFalse(wildcardSession.hasPermission(repositories, "/b/b"));
        Assertions.assertFalse(wildcardSession.hasPermission(repositories, "/x/b/c"));
    }

    @Test
    void hasRootPermission() {
        List<String> repositories = Arrays.asList("releases", "snapshots");
        Session standardRootSession = new Session(new Token("/", "alias", "token"));
        Session wildcardRootSession = new Session(new Token("*", "alias", "token"));

        Assertions.assertTrue(standardRootSession.hasPermission(repositories, "/"));
        Assertions.assertFalse(wildcardRootSession.hasPermission(repositories, "/"));
        Assertions.assertTrue(wildcardRootSession.hasPermission(repositories, "/releases"));
        Assertions.assertTrue(wildcardRootSession.hasPermission(repositories, "/snapshots"));
    }

}