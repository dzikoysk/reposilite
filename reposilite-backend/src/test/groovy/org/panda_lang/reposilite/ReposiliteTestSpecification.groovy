/*
 * Copyright (c) 2021 dzikoysk
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

package org.panda_lang.reposilite

import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
class ReposiliteTestSpecification extends ReposiliteTestSpecificationExtension {

    private static final String TINYLOG_WRITER_PROPERTY = 'tinylog.writerFile.file'

    @TempDir
    protected Path workingDirectory
    protected Reposilite reposilite

    @BeforeEach
    protected void before() throws Exception {
        try {
            System.setProperty(TINYLOG_WRITER_PROPERTY, 'target/log.txt')
            this.reposilite = new Reposilite(workingDirectory.resolve(ReposiliteConstants.CONFIGURATION_FILE_NAME), workingDirectory, false, true)

            def from = Paths.get("src/test/workspace/repositories")
            def to = workingDirectory.resolve("repositories")

            Files.walk(from).forEach(source -> {
                Path destination = source.relativize(from)
                if (destination.parent != null && !Files.exists(destination.parent)) {
                    Files.createDirectories(destination.parent)
                }

                if (!Files.exists(destination)) {
                    Files.copy(source, to.resolve(destination))
                }
            })

            this.reposilite.repositoryService.load(this.reposilite.configuration)

            for (def configuration : reposilite.configurations()) {
                configuration.configure(reposilite)
            }
        } finally {
            System.clearProperty(TINYLOG_WRITER_PROPERTY)
        }
    }

    protected boolean executeCommand(String name) {
        return reposilite.getConsole().defaultExecute(name)
    }

}
