package org.panda_lang.reposilite.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationDtoTest {

    private final ConfigurationDto configurationDto = new ConfigurationDto("title", "description", "accentColor");

    @Test
    void getAccentColor() {
        assertEquals("accentColor", configurationDto.getAccentColor());
    }

    @Test
    void getDescription() {
        assertEquals("description", configurationDto.getDescription());
    }

    @Test
    void getTitle() {
        assertEquals("title", configurationDto.getTitle());
    }

}