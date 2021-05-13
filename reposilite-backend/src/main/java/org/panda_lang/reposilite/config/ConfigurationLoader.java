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

import net.dzikoysk.cdn.Cdn;
import net.dzikoysk.cdn.CdnFactory;
import net.dzikoysk.dynamiclogger.Journalist;
import net.dzikoysk.dynamiclogger.Logger;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.ClassUtils;
import org.panda_lang.utilities.commons.StringUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;

public final class ConfigurationLoader implements Journalist {

    private final Journalist journalist;

    public ConfigurationLoader(Journalist journalist) {
        this.journalist = journalist;
    }

    public Configuration tryLoad(Path customConfigurationFile) {
        try {
            return load(customConfigurationFile);
        } catch (Exception exception) {
            throw new RuntimeException("Cannot load configuration", exception);
        }
    }

    public Configuration load(Path configurationFile) throws Exception {
        if (!FilesUtils.getExtension(configurationFile.getFileName().toString()).equals("cdn")) {
            throw new IllegalArgumentException("Custom configuration file does not have '.cdn' extension");
        }

        Cdn cdn = CdnFactory.createStandard();

        Configuration configuration = Files.exists(configurationFile)
            ? cdn.load(new String(Files.readAllBytes(configurationFile), StandardCharsets.UTF_8), Configuration.class)
            : createConfiguration(configurationFile);

        verifyBasePath(configuration);
        verifyProxied(configuration);
        Files.write(configurationFile, cdn.render(configuration).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        loadProperties(configuration);

        return configuration;
    }

    private Configuration createConfiguration(Path configurationFile) throws Exception {
        Path legacyConfiguration = configurationFile.resolveSibling(configurationFile.getFileName().toString().replace(".cdn", ".yml"));

        if (!Files.exists(legacyConfiguration)) {
            getLogger().info("Generating default configuration file.");
            return new Configuration();
        }

        getLogger().info("Legacy configuration file has been found");
        Configuration configuration = CdnFactory.createYamlLike().load(new String(Files.readAllBytes(configurationFile), StandardCharsets.UTF_8), Configuration.class);
        getLogger().info("YAML configuration has been converted to CDN format");
        Files.delete(legacyConfiguration);

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

    private void verifyProxied(Configuration configuration) {
        for (int index = 0; index < configuration.proxied.size(); index++) {
            String proxied = configuration.proxied.get(index);

            if (proxied.endsWith("/")) {
                configuration.proxied.set(index, proxied.substring(0, proxied.length() - 1));
            }
        }
    }

    private void loadProperties(Configuration configuration) {
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
                getLogger().info("Unsupported type: " + type + " for " + custom);
                continue;
            }

            try {
                declaredField.set(configuration, customValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot modify configuration value", e);
            }
        }
    }

    @Override
    public Logger getLogger() {
        return journalist.getLogger();
    }

}
