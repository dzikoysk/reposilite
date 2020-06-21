package org.panda_lang.reposilite.frontend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendServiceTest {

    private static final FrontendService FRONTEND_SERVICE = FrontendService.load();

    @Test
    void forMessage() {
        assertTrue(FRONTEND_SERVICE.forMessage("#onlypanda").contains("#onlypanda"));
        assertFalse(FRONTEND_SERVICE.forMessage("#onlyreposilite").contains("#onlypanda"));
        assertTrue(FRONTEND_SERVICE.forMessage("#onlyreposilite").contains("#onlyreposilite"));
    }

    @Test
    void getApp() {
        assertTrue(FRONTEND_SERVICE.getApp().contains("Vue"));
    }

}