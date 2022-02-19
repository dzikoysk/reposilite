/*
 * Copyright (c) 2022 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     *
     * @param loader the loader instance used to load this plugin
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