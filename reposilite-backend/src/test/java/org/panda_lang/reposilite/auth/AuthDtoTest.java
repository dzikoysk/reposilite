package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthDtoTest {

    private static final AuthDto AUTH_DTO = new AuthDto(true, "associated_path", Collections.singletonList("releases"));

    @Test
    void getRepositories() {
        assertEquals(Collections.singletonList("releases"), AUTH_DTO.getRepositories());
    }

    @Test
    void getPath() {
        assertEquals("associated_path", AUTH_DTO.getPath());
    }

    @Test
    void isManager() {
        assertTrue(AUTH_DTO.isManager());
    }

}