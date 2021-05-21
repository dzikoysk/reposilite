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

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpResponse
import com.google.api.client.http.javanet.NetHttpTransport
import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.utilities.commons.ArrayUtils

@CompileStatic
abstract class ReposiliteIntegrationTestSpecification extends ReposiliteTestSpecificationExtension {

    public static final String PORT = String.valueOf(new Random().nextInt(16383) + 49151)
    public static final HttpRequestFactory REQUEST_FACTORY = new NetHttpTransport().createRequestFactory()

    @TempDir
    protected File workingDirectory
    protected Map<String, String> properties = new HashMap<>()
    protected Reposilite reposilite

    @BeforeEach
    protected void before() {
        System.setProperty("tinylog.writerFile.file", "target/log.txt")
        ReposiliteWriter.clear()
        reposilite = reposilite(workingDirectory)
        reposilite.launch()
    }

    protected Reposilite reposilite(File workingDirectory, String... args) {
        return reposilite(PORT, workingDirectory, args)
    }

    protected Reposilite reposilite(String port, File workingDirectory, String... args) {
        FileUtils.copyDirectory(new File("src/test/workspace/repositories"), new File(workingDirectory, "repositories"))
        System.setProperty("reposilite.port", port)
        properties.forEach({ property, value -> System.setProperty(property, value) })

        try {
            return ReposiliteLauncher.create(ArrayUtils.merge(args, ArrayUtils.of(
                    "--working-directory=" + workingDirectory.getAbsolutePath(),
                    "--test-env"
            ))).orElseThrow({ new RuntimeException("Invalid test parameters") })
        }
        finally {
            System.clearProperty("reposilite.port")
            properties.forEach({ key, value -> System.clearProperty(key) })
        }
    }

    @AfterEach
    protected void after() {
        reposilite.forceShutdown()
    }

    protected static HttpResponse getRequest(String uri) {
        return REQUEST_FACTORY.buildGetRequest(url(uri))
                .setThrowExceptionOnExecuteError(false)
                .execute()
    }

    protected static HttpResponse getAuthenticated(String uri, String username, String password) {
        HttpRequest request = REQUEST_FACTORY.buildGetRequest(url(uri))
        request.setThrowExceptionOnExecuteError(false)
        request.getHeaders().setBasicAuthentication(username, password)
        return request.execute()
    }

    protected static GenericUrl url(String uri) {
        return new GenericUrl("http://localhost:" + PORT + uri)
    }

}
