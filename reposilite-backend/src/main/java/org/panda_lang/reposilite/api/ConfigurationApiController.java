package org.panda_lang.reposilite.api;

import io.javalin.http.Context;
import org.panda_lang.reposilite.RepositoryController;
import org.panda_lang.reposilite.config.Configuration;

public final class ConfigurationApiController implements RepositoryController {

    private final Configuration configuration;

    public ConfigurationApiController(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Context handleContext(Context ctx) {
        return ctx.json(new ConfigDto(
                configuration.getTitle(),
                configuration.getDescription(),
                configuration.getAccentColor())
        );
    }

}
