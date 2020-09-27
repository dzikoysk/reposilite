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

import groovy.transform.CompileStatic
import net.dzikoysk.cdn.CDN
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.ReposiliteConstants
import org.panda_lang.reposilite.ReposiliteContext
import org.panda_lang.reposilite.ReposiliteIntegrationTestSpecification
import org.panda_lang.reposilite.ReposiliteLauncher
import org.panda_lang.reposilite.config.Configuration
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.utilities.commons.FileUtils

import java.util.concurrent.ExecutorService

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
final class ProxyServiceTest extends ReposiliteIntegrationTestSpecification {

    @TempDir
    protected File PROXIED_WORKING_DIRECTORY

    private int proxiedPort = Integer.parseInt(PORT) - 1
    private ExecutorService executorService
    private FailureService failureService
    private ProxyService proxyService

    @BeforeEach
    void configure() throws IOException {
        super.reposilite.getConfiguration().proxied = Collections.singletonList('http://localhost/')

        this.executorService = super.reposilite.getIoService()
        this.failureService = super.reposilite.getFailureService()
        this.proxyService = new ProxyService(
                true,
                true,
                [
                        'http://unknown-repository.site/',
                        'http://127.0.0.1:' + proxiedPort
                ],
                super.reposilite.getIoService(),
                failureService,
                super.reposilite.getRepositoryService())

        def proxiedFile = new File(super.workingDirectory, '/repositories/releases/proxiedGroup/proxiedArtifact/proxied.pom')
        proxiedFile.getParentFile().mkdirs()
        proxiedFile.createNewFile()
        FileUtils.overrideFile(proxiedFile, 'proxied content')
    }

    @Test
    void 'should return error for invalid proxied request' () {
        def result = proxyService.findProxied(context('/groupId/artifactId'))
        assertTrue result.containsError()
        assertEquals 'Invalid proxied request', result.getError().getMessage()
    }

    @Test
    void 'should return 404 and artifact not found' () throws Exception {
        def error = proxyService.findProxied(context('/releases/proxiedGroup/proxiedArtifact/notfound.pom')).getValue().get().getError()
        assertNotNull error
        assertEquals 'Artifact not found in local and remote repository', error.message
    }

    @Test
    void 'should return 200 and proxied file' () throws Exception {
        executorService.submit({
            return proxyService.findProxied(context('/releases/proxiedGroup/proxiedArtifact/proxied.pom')).getValue().get().getValue()
        }).get()
    }

    @Test
    void 'should proxy remote file' () {
        def proxiedConfiguration = new Configuration()
        proxiedConfiguration.port = proxiedPort

        def proxiedConfigurationFile = new File(PROXIED_WORKING_DIRECTORY.getAbsolutePath() + '/' + ReposiliteConstants.CONFIGURATION_FILE_NAME)
        proxiedConfigurationFile.getParentFile().mkdirs()
        FileUtils.overrideFile(proxiedConfigurationFile, CDN.defaultInstance().compose(proxiedConfiguration))

        def proxiedReposilite = ReposiliteLauncher.create(null, PROXIED_WORKING_DIRECTORY.getAbsolutePath(), true)

        try {
            proxiedReposilite.launch()

            def proxiedFile = new File(PROXIED_WORKING_DIRECTORY.getAbsolutePath() + '/repositories/releases/g/a/file.txt')
            proxiedFile.getParentFile().mkdirs()
            FileUtils.overrideFile(proxiedFile, 'test')

            def result = proxyService
                    .findProxied(context('/releases/g/a/file.txt'))
                    .getValue()
                    .get()
                    .getValue()

            assertTrue result.isAttachment()
            assertEquals 'text/plain', result.getContentType().get()
        } finally {
            proxiedReposilite.shutdown()
        }
    }

    private static ReposiliteContext context(String uri) {
        return new ReposiliteContext(uri, 'GET', 'address', [:], { new ByteArrayInputStream() }, { new ByteArrayOutputStream(1024) })
    }

}
