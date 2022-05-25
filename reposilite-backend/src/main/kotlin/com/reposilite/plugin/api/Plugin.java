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

import com.reposilite.ReposilitePropertiesKt;
import com.reposilite.configuration.shared.SharedSettings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents plugin metadata, should be used to annotate {@link com.reposilite.plugin.api.ReposilitePlugin}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Plugin {

    /**
     * @return the name of this plugin
     */
    String name();

    /**
     * @return version of plugin, by default it is same as the current Reposilite version
     * @see com.reposilite.ReposilitePropertiesKt#VERSION
     */
    String version() default ReposilitePropertiesKt.VERSION;

    /**
     * @return array of plugins required to launch before this one
     */
    String[] dependencies() default {};

    Class<? extends SharedSettings> settings() default SharedSettings.class;

}
