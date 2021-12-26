package com.reposilite.plugin.api;

import com.reposilite.journalist.Journalist;
import com.reposilite.journalist.Logger;
import com.reposilite.plugin.Extensions;
import com.reposilite.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is the bootstrap-like abstract class to use by each plugin to properly initialize context for extensions.
 */
public abstract class ReposilitePlugin implements Journalist {

    /**
     * The supervisor of this plugin
     */
    @SuppressWarnings("unused")
    Extensions extensions;

    /**
     * Called when plugin is loaded using {@link com.reposilite.plugin.PluginLoader}.
     * There is no guarantee of order of load, so you cannot relay on other dependencies here.
     */
    public void load(@NotNull PluginLoader loader) {}

    /**
     * The heart of your plugin, that's where every plugin can start to interact with Reposilite API.
     *
     * @return a facade - the api provider. If the given plugin don't want to register its own api, it may respond with null value
     */
    public abstract @Nullable Facade initialize();

    /**
     * @return the supervisor of this plugin
     */
    public @NotNull Extensions extensions() {
        return extensions;
    }

    /**
     * @return the assigned for this plugin logger
     */
    public @NotNull Logger getLogger() {
        return extensions().getLogger();
    }

}