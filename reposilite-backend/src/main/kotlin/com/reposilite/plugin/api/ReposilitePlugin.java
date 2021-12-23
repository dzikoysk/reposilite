package com.reposilite.plugin.api;

import com.reposilite.journalist.Journalist;
import com.reposilite.journalist.Logger;
import com.reposilite.plugin.ExtensionsManagement;
import org.jetbrains.annotations.Nullable;

/**
 * This is the bootstrap-like abstract class to use by each plugin to properly initialize context for extensions.
 */
public abstract class ReposilitePlugin implements Journalist {

    @SuppressWarnings("unused")
    ExtensionsManagement extensionsManagement;

    /**
     * The heart of your plugin, that's where every plugin can start to interact with Reposilite API.
     *
     * @return a facade - the api provider. If the given plugin don't want to register its own api, it may respond with null value
     */
    public abstract @Nullable Facade initialize();

    /**
     * Utility method that allows to inject {@link com.reposilite.plugin.ExtensionsManagement} instance.
     * This method should not be called in production environment by plugin, but it may be useful in test env.
     * The method can be called only once, so you cannot override already assigned {@link com.reposilite.plugin.ExtensionsManagement} instance.
     *
     * @param extensionsManagement the supervisor of this plugin
     */
    public void setExtensionsManagement(ExtensionsManagement extensionsManagement) {
        if (extensionsManagement == null) {
            throw new IllegalStateException("Plugin has been already initialized with ExtensionManagement");
        }
        this.extensionsManagement = extensionsManagement;
    }

    /**
     * @return the supervisor of this plugin
     */
    public ExtensionsManagement getExtensionsManagement() {
        return extensionsManagement;
    }

    /**
     * @return the assigned for this plugin logger
     */
    public Logger getLogger() {
        return getExtensionsManagement().getLogger();
    }

}