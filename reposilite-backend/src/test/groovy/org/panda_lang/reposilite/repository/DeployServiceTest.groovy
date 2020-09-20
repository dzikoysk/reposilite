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

package org.panda_lang.reposilite.repository

import org.apache.http.HttpStatus
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteContext
import org.panda_lang.reposilite.ReposiliteTest
import org.panda_lang.utilities.commons.FileUtils

import java.nio.channels.FileChannel
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class DeployServiceTest extends ReposiliteTest {

    @Test
    void 'should respect disk quota' () {
        def deployService = new DeployService(
                true,
                super.reposilite.authenticator,
                new RepositoryService(super.workingDirectory.getAbsolutePath(), '0MB'),
                super.reposilite.metadataService,
                super.reposilite.failureService,
                super.reposilite.executorService)

        super.reposilite.tokenService.createToken('/', 'user', 'secret')
        def auth = [ 'Authorization': 'Basic ' + 'user:secret'.bytes.encodeBase64() ]

        def context = new ReposiliteContext('/releases/a/b/c.txt', 'POST', '', auth, { new ByteArrayInputStream('test'.bytes) }, {})
        def result = deployService.deploy(context)
        assertTrue result.containsError()

        def error = result.getError()
        assertEquals HttpStatus.SC_INSUFFICIENT_STORAGE, error.status
        assertEquals 'Out of disk space', error.message
    }

    @Test
    void 'should retry deployment of locked file' () {
        def deployService = super.reposilite.getDeployService()

        def file = new File(super.workingDirectory.getAbsolutePath() + '/releases/a/b/c.txt'.replace('/', File.separator))
        file.getParentFile().mkdirs()
        FileUtils.overrideFile(file, 'test')

        def channel = FileChannel.open(file.toPath(), [ StandardOpenOption.WRITE ] as OpenOption[])
        def lock = channel.lock()

        new Thread({
            Thread.sleep(deployService.RETRY_WRITE_TIME)
            lock.release()
        }).start()

        def context = new ReposiliteContext('/releases/a/b/c.txt', 'POST', '', [:], { new ByteArrayInputStream('test'.bytes) }, {})
        assertTrue deployService.writeFile(context, FileDetailsDto.of(file), file).get().isDefined()
    }

}
