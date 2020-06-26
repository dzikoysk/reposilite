/*
 * Copyright (c) 2020 Dzikoysk
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

package org.panda_lang.reposilite.config;

import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.YamlUtils;
import org.panda_lang.utilities.commons.ClassUtils;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;

public final class ConfigurationLoader {

    public static Configuration load(String workingDirectory) {
        File configurationFile = new File(workingDirectory, ReposiliteConstants.CONFIGURATION_FILE_NAME);

        if (!configurationFile.exists()) {
            Reposilite.getLogger().info("Generating default configuration file.");

            try {
                FilesUtils.copyResource("/" + ReposiliteConstants.CONFIGURATION_FILE_NAME, configurationFile);
            } catch (IOException exception) {
                throw new RuntimeException("Cannot create configuration file, ", exception);
            }
        }
        else {
            Reposilite.getLogger().info("Using an existing configuration file");
        }

        Reposilite.getLogger().info("");

        return YamlUtils.forceLoad(configurationFile, Configuration.class, properties -> {
            for (Entry<String, Object> property : properties.entrySet()) {
                String custom = System.getProperty("reposilite." + property.getKey());

                if (StringUtils.isEmpty(custom) || property.getValue() == null) {
                    continue;
                }

                Class<?> type = ClassUtils.getNonPrimitiveClass(property.getValue().getClass());
                Object customValue;

                if (String.class == type) {
                    customValue = custom;
                }
                else if (Integer.class == type) {
                    customValue = Integer.parseInt(custom);
                }
                else if (Boolean.class == type) {
                    customValue = Boolean.parseBoolean(custom);
                }
                else if (Collection.class.isAssignableFrom(type)) {
                    customValue = Arrays.asList(custom.split(","));
                }
                else {
                    Reposilite.getLogger().info("Unsupported type: " + type + " for " + custom);
                    continue;
                }

                property.setValue(customValue);
            }

            return properties;
        });
    }

}
