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

import net.dzikoysk.cdn.CDN;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.ClassUtils;
import org.panda_lang.utilities.commons.FileUtils;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

public final class ConfigurationLoader {

    public static Configuration tryLoad(String customConfigurationFile, String workingDirectory) {
        try {
            return load(customConfigurationFile, workingDirectory);
        } catch (Exception exception) {
            throw new RuntimeException("Cannot load configuration", exception);
        }
    }

    public static Configuration load(String customConfigurationFile, String workingDirectory) throws Exception {
        File configurationFile = StringUtils.isEmpty(customConfigurationFile)
            ? new File(workingDirectory, ReposiliteConstants.CONFIGURATION_FILE_NAME)
            : new File(customConfigurationFile);

        if (!FilesUtils.getExtension(configurationFile.getName()).equals("cdn")) {
            throw new IllegalArgumentException("Custom configuration file does not have '.cdn' extension");
        }

        CDN cdn = CDN.defaultInstance();

        Configuration configuration = configurationFile.exists()
            ? cdn.parse(Configuration.class, FileUtils.getContentOfFile(configurationFile))
            : createConfiguration(configurationFile);

        verifyBasePath(configuration);
        verifyProxied(configuration);
        FileUtils.overrideFile(configurationFile, cdn.render(configuration));
        loadProperties(configuration);

        return configuration;
    }

    private static Configuration createConfiguration(File configurationFile) throws Exception {
        File legacyConfiguration = new File(configurationFile.getAbsolutePath().replace(".cdn", ".yml"));

        if (!legacyConfiguration.exists()) {
            Reposilite.getLogger().info("Generating default configuration file.");
            return new Configuration();
        }

        Reposilite.getLogger().info("Legacy configuration file has been found");

        Configuration configuration = CDN.configure()
                .enableYamlLikeFormatting()
                .build()
                .parse(Configuration.class, FileUtils.getContentOfFile(legacyConfiguration));

        Reposilite.getLogger().info("YAML configuration has been converted to CDN format");
        FileUtils.delete(legacyConfiguration);
        return configuration;
    }

    private static void verifyBasePath(Configuration configuration) {
        String basePath = configuration.basePath;

        if (!StringUtils.isEmpty(basePath)) {
            if (!basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }

            if (!basePath.endsWith("/")) {
                basePath += "/";
            }

            configuration.basePath = basePath;
        }
    }

    private static void verifyProxied(Configuration configuration) {
        for (int index = 0; index < configuration.proxied.size(); index++) {
            String proxied = configuration.proxied.get(index);

            if (proxied.endsWith("/")) {
                configuration.proxied.set(index, proxied.substring(0, proxied.length() - 1));
            }
        }
    }

    private static void loadProperties(Configuration configuration) {
        for (Field declaredField : configuration.getClass().getDeclaredFields()) {
            String custom = System.getProperty("reposilite." + declaredField.getName());

            if (StringUtils.isEmpty(custom)) {
                continue;
            }

            Class<?> type = ClassUtils.getNonPrimitiveClass(declaredField.getType());
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

            try {
                declaredField.set(configuration, customValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot modify configuration value", e);
            }
        }
    }

}
