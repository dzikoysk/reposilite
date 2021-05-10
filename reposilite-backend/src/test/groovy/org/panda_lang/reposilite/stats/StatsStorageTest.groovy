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

package org.panda_lang.reposilite.stats

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.ReposiliteConstants
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.reposilite.storage.FileSystemStorageProvider
import org.panda_lang.utilities.commons.FileUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
final class StatsStorageTest {

    @TempDir
    public Path workingDirectory

    @Test
    void 'should convert old data file' () {
        def statsStorage = new StatsStorage(workingDirectory, new FailureService(), FileSystemStorageProvider.of(Paths.get(""), "10GB"))

        Files.write(workingDirectory.resolve("stats.yml"), 'records: {}'.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        statsStorage.loadStoredStats().get()

        def dataFile = workingDirectory.resolve(ReposiliteConstants.STATS_FILE_NAME)
        assertTrue Files.exists(dataFile)
        assertEquals 'records: {}', new String(Files.readAllBytes(dataFile), StandardCharsets.UTF_8)
    }

}
