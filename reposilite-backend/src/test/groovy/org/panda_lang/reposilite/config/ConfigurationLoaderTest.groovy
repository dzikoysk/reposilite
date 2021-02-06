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
import org.panda_lang.utilities.commons.FileUtils
import org.panda_lang.utilities.commons.text.Joiner

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class ConfigurationLoaderTest {

    @TempDir
    protected File workingDirectory

    @Test
    void 'should load file with custom properties' () {
        try {
            System.setProperty("reposilite.hostname", "localhost")          // String type
            System.setProperty("reposilite.port", "8080")                   // Integer type
            System.setProperty("reposilite.debugEnabled", "true")           // Boolean type
            System.setProperty("reposilite.proxied", "http://a.com,b.com")  // List<String> type
            System.setProperty("reposilite.repositories", " ")              // Skip empty

            def configuration = ConfigurationLoader.tryLoad("", workingDirectory.getAbsolutePath())
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

        def configuration = ConfigurationLoader.tryLoad("", workingDirectory.getAbsolutePath())
        assertEquals 80, configuration.port
    }

    @Test
    void 'should load custom config' () {
        def customConfig = new File(workingDirectory, "random.cdn")
        FileUtils.overrideFile(customConfig, CDN.defaultInstance().render(new Configuration()))
        FileUtils.overrideFile(customConfig, FileUtils.getContentOfFile(customConfig).replace("port: 80", "port: 7"))

        def configuration = ConfigurationLoader.tryLoad(customConfig.getAbsolutePath(), workingDirectory.getAbsolutePath())
        assertEquals 7, configuration.port
    }

    @Test
    void 'should not load other file types' () {
        def customConfig = new File(workingDirectory, "random.properties")
        FileUtils.overrideFile(customConfig, CDN.defaultInstance().render(new Configuration()))
        assertThrows RuntimeException.class, { ConfigurationLoader.load(customConfig.getAbsolutePath(), workingDirectory.getAbsolutePath()) }
    }

    @Test
    void 'should convert legacy config' () {
        def legacyConfig = new File(workingDirectory, "config.yml")
        FileUtils.overrideFile(legacyConfig, "port: 7")
        def config = new File(legacyConfig.getAbsolutePath().replace(".yml", ".cdn"))

        def configuration = ConfigurationLoader.tryLoad(config.getAbsolutePath(), workingDirectory.getAbsolutePath())
        assertEquals 7, configuration.port
        assertTrue config.exists()
        assertFalse legacyConfig.exists()
    }

    @Test
    void 'should verify proxied' () {
        def config = new File(workingDirectory, "config.cdn")
        FileUtils.overrideFile(config, Joiner.on("\n").join(
                "proxied {",
                "  https://without.slash",
                "  https://with.slash/",
                "}"
        ).toString())

        def configuration = ConfigurationLoader.tryLoad(config.getAbsolutePath(), workingDirectory.getAbsolutePath())
        assertEquals(Arrays.asList("https://without.slash", "https://with.slash"), configuration.proxied)
    }

}