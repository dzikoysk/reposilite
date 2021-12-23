package com.reposilite.test;

import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.Plugin;
import com.reposilite.plugin.api.ReposiliteInitializeEvent;
import com.reposilite.plugin.api.ReposilitePlugin;
import org.jetbrains.annotations.Nullable;

@Plugin(name = "test")
public final class TestPlugin extends ReposilitePlugin {

    @Override
    public @Nullable Facade initialize() {
        getExtensionsManagement().registerEvent(ReposiliteInitializeEvent.class, event -> {
            getLogger().info("");
            getLogger().info("--- Test plugin");
            getLogger().info("Test plugin has been properly loaded");
        });
        return null;
    }

}
