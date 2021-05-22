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

package org.panda_lang.reposilite.maven.repository

import groovy.transform.CompileStatic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.web.ReposiliteContext
import org.panda_lang.reposilite.ReposiliteIntegrationTestSpecification
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.reposilite.storage.infrastructure.FileSystemStorageProvider
import org.panda_lang.utilities.commons.FileUtils

import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
final class ProxyServiceTest extends ReposiliteIntegrationTestSpecification {

    @TempDir
    protected Path PROXIED_WORKING_DIRECTORY

    private int proxiedPort = Integer.parseInt(PORT) - 1
    private FailureService failureService
    private ProxyService proxyService

    @BeforeEach
    void configure() throws IOException {
        super.reposilite.getConfiguration().proxied = Collections.singletonList('http://localhost:' + PORT)

        this.failureService = super.reposilite.getFailureService()
        this.proxyService = new ProxyService(
                true,
                true,
                1000,
                1000,
                [
                        'http://unknown-repository.site/',
                        'http://127.0.0.1:' + proxiedPort
                ]
                ,
                super.reposilite.getRepositoryService(),
                failureService,
                FileSystemStorageProvider.of(Paths.get(""), "10GB"))

        def proxiedFile = new File(super.workingDirectory, '/repositories/releases/proxiedGroup/proxiedArtifact/proxied.pom')
        proxiedFile.getParentFile().mkdirs()
        proxiedFile.createNewFile()
        FileUtils.overrideFile(proxiedFile, 'proxied content')
    }

    @Test
    void 'should return error for invalid proxied request' () {
        def result = proxyService.findProxied(context('/groupId/artifactId'))
        assertTrue result.isErr()
        assertEquals 'Invalid proxied request', result.getError().getMessage()
    }

    @Test
    void 'should return 404 and artifact not found' () throws Exception {
        def uri = '/proxiedGroup/proxiedArtifact/notfound.pom'
        def error = proxyService.findProxied(context(uri)).getError()
        assertNotNull error
        assertEquals 'Artifact ' + uri + ' not found', error.message
    }

    private static ReposiliteContext context(String uri) {
        return new ReposiliteContext(uri, 'GET', 'address', [:], { new ByteArrayInputStream() })
    }

}
