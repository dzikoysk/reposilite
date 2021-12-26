package com.reposilite.plugin.example;

import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.Plugin;
import com.reposilite.plugin.api.ReposiliteInitializeEvent;
import com.reposilite.plugin.api.ReposilitePlugin;
import org.jetbrains.annotations.Nullable;

@Plugin(name = "example")
public final class ExamplePlugin extends ReposilitePlugin {

    @Override
    public @Nullable Facade initialize() {
        extensions().registerEvent(ReposiliteInitializeEvent.class, event -> {
            getLogger().info("");
            getLogger().info("--- Example plugin");
            getLogger().info("Example plugin has been properly loaded");
        });
        return null;
    }

}
