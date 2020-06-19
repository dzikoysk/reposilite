package org.panda_lang.reposilite.api;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorDtoTest {

    private static final ErrorDto ERROR_DTO = new ErrorDto(HttpStatus.SC_NOT_FOUND, "Message");

    @Test
    void getStatus() {
        assertEquals(HttpStatus.SC_NOT_FOUND, ERROR_DTO.getStatus());
    }

    @Test
    void getMessage() {
        assertEquals("Message", ERROR_DTO.getMessage());
    }

}