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

package org.panda_lang.reposilite.maven.repository

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteContext
import org.panda_lang.reposilite.ReposiliteTestSpecification

import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption

@CompileStatic
class RepositoryServiceTest extends ReposiliteTestSpecification {

    @Test
    void 'should retry deployment of locked file' () {
        def repositoryService = super.reposilite.getRepositoryService()

        def path = super.workingDirectory.resolve('releases').resolve('a').resolve('b').resolve('c.txt')
        Files.createDirectories(path.parent)

        Files.write(path, "test".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        def channel = FileChannel.open(path, [ StandardOpenOption.WRITE ] as OpenOption[])
        def lock = channel.lock()

        new Thread({
            Thread.sleep(1000L)
            lock.release()
        }).start()

        def context = new ReposiliteContext('/releases/a/b/c.txt', 'POST', '', [:], { new ByteArrayInputStream('test'.bytes) })
    }
}
