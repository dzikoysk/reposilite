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

package org.panda_lang.reposilite.config

import groovy.transform.CompileStatic
import net.dzikoysk.cdn.CDN
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.ReposiliteConstants
import org.panda_lang.utilities.commons.FileUtils
import org.panda_lang.utilities.commons.text.Joiner

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class ConfigurationLoaderTest {

    @TempDir
    protected Path workingDirectory

    @Test
    void 'should load file with custom properties' () {
        try {
            System.setProperty("reposilite.hostname", "localhost")          // String type
            System.setProperty("reposilite.port", "8080")                   // Integer type
            System.setProperty("reposilite.debugEnabled", "true")           // Boolean type
            System.setProperty("reposilite.proxied", "http://a.com,b.com")  // List<String> type
            System.setProperty("reposilite.repositories", " ")              // Skip empty

            def configuration = ConfigurationLoader.tryLoad(workingDirectory.resolve(ReposiliteConstants.CONFIGURATION_FILE_NAME))
            assertEquals "localhost", configuration.hostname
            assertEquals 8080, configuration.port
            assertTrue configuration.debugEnabled
            assertEquals Arrays.asList("http://a.com", "b.com"), configuration.proxied
            assertFalse configuration.repositories.isEmpty()
        }
        finally {
            // Clean up the system properties to avoid loading of these values by the further tests
            System.clearProperty("reposilite.hostname")
            System.clearProperty("reposilite.port")
            System.clearProperty("reposilite.debugEnabled")
            System.clearProperty("reposilite.proxied")
            System.clearProperty("reposilite.repositories")
        }
    }

    @Test
    void 'should load custom config' () {
        def customConfig = workingDirectory.resolve("random.cdn")
        Files.write(customConfig, CDN.defaultInstance().render(new Configuration()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        Files.write(customConfig, new String(Files.readAllBytes(customConfig), StandardCharsets.UTF_8).replace("port: 80", "port: 7").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        def configuration = ConfigurationLoader.tryLoad(customConfig)
        assertEquals 7, configuration.port
    }

    @Test
    void 'should not load other file types' () {
        def customConfig = workingDirectory.resolve("random.properties")
        Files.write(customConfig, CDN.defaultInstance().render(new Configuration()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        assertThrows RuntimeException.class, { ConfigurationLoader.load(customConfig) }
    }

    @Test
    void 'should convert legacy config' () {
        def legacyConfig = workingDirectory.resolve("config.yml")
        Files.write(legacyConfig, "port: 7".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        def config = legacyConfig.getParent().resolve(legacyConfig.getFileName().toString().replace(".yml", ".cdn"))

        def configuration = ConfigurationLoader.tryLoad(config)
        assertEquals 7, configuration.port
        assertTrue Files.exists(config)
        assertFalse Files.exists(legacyConfig)
    }

    @Test
    void 'should verify proxied' () {
        def config = workingDirectory.resolve("config.cdn")
        Files.write(config, Joiner.on("\n").join(
                "proxied {",
                "  https://without.slash",
                "  https://with.slash/",
                "}"
        ).toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        def configuration = ConfigurationLoader.tryLoad(config)
        assertEquals(Arrays.asList("https://without.slash", "https://with.slash"), configuration.proxied)
    }

}