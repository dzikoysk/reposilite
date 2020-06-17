package org.panda_lang.reposilite.config;

import java.io.Serializable;

final class ConfigurationDto implements Serializable {

    private final String title;
    private final String description;
    private final String accentColor;

    ConfigurationDto(String title, String description, String accentColor) {
        this.title = title;
        this.description = description;
        this.accentColor = accentColor;
    }

    public String getAccentColor() {
        return accentColor;
    }


    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }
}
