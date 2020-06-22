package org.panda_lang.reposilite.repository;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.utilities.commons.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepositoryUtilsTest {

    private static final Configuration CONFIGURATION = new Configuration();

    @Test
    void shouldNotInterfere() {
        assertEquals("releases/without/repo/", RepositoryUtils.normalizeUri(CONFIGURATION, "releases/without/repo/"));
    }

    @Test
    void shouldRewritePath() {
        assertEquals("releases/without/repo/", RepositoryUtils.normalizeUri(CONFIGURATION, "/without/repo/"));
    }

    @Test
    void shouldNotAllowPathEscapes() {
        assertEquals(StringUtils.EMPTY, RepositoryUtils.normalizeUri(CONFIGURATION, "~/home"));
        assertEquals(StringUtils.EMPTY, RepositoryUtils.normalizeUri(CONFIGURATION, "../../../../monkas"));
    }

    @Test
    void shouldNotRewritePaths() {
        Configuration configuration = new Configuration();
        configuration.setRewritePathsEnabled(false);

        assertEquals("without/repo/", RepositoryUtils.normalizeUri(configuration, "without/repo/"));
    }

}