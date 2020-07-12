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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.reposilite.ReposiliteConstants;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLoaderTest {

    @TempDir
    protected File workingDirectory;

    @Test
    void shouldLoadFileWithCustomProperties() {
        try {
            System.setProperty("reposilite.hostname", "localhost");         // String type
            System.setProperty("reposilite.port", "8080");                  // Integer type
            System.setProperty("reposilite.debugEnabled", "true");          // Boolean type
            System.setProperty("reposilite.proxied", "http://a.com,b.com"); // List<String> type
            System.setProperty("reposilite.repositories", " ");              // Skip empty

            Configuration configuration = ConfigurationLoader.load("", workingDirectory.getAbsolutePath());
            assertEquals("localhost", configuration.getHostname());
            assertEquals(8080, configuration.getPort());
            assertTrue(configuration.isDebugEnabled());
            assertEquals(Arrays.asList("http://a.com", "b.com"), configuration.getProxied());
            assertFalse(configuration.getRepositories().isEmpty());
        }
        finally {
            // Clean up the system properties to avoid loading of these values by the further tests
            System.clearProperty("reposilite.hostname");
            System.clearProperty("reposilite.port");
            System.clearProperty("reposilite.debugEnabled");
            System.clearProperty("reposilite.proxied");
            System.clearProperty("reposilite.repositories");
        }

        Configuration configuration = ConfigurationLoader.load("", workingDirectory.getAbsolutePath());
        assertEquals(80, configuration.getPort());
    }

    @Test
    void shouldLoadCustomConfig() throws IOException {
        File customConfig = new File(workingDirectory, "random.yml");
        FilesUtils.copyResource("/" + ReposiliteConstants.CONFIGURATION_FILE_NAME, customConfig);
        FileUtils.overrideFile(customConfig, FileUtils.getContentOfFile(customConfig).replace("port: 80", "port: 7"));

        Configuration configuration = ConfigurationLoader.load(customConfig.getAbsolutePath(), workingDirectory.getAbsolutePath());
        assertEquals(7, configuration.getPort());
    }

    @Test
    void shouldNotLoadOtherFileTypes() throws IOException {
        File customConfig = new File(workingDirectory, "random.properties");
        FilesUtils.copyResource("/" + ReposiliteConstants.CONFIGURATION_FILE_NAME, customConfig);

        assertThrows(IllegalArgumentException.class, () -> ConfigurationLoader.load(customConfig.getAbsolutePath(), workingDirectory.getAbsolutePath()));
    }

}