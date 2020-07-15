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

package org.panda_lang.reposilite.repository;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import io.javalin.http.Context;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.frontend.FrontendService;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.FutureUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.collection.Pair;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LookupService {

    private final Reposilite reposilite;
    private final FrontendService frontend;
    private final Configuration configuration;
    private final Authenticator authenticator;
    private final MetadataService metadataService;
    private final RepositoryService repositoryService;
    private final HttpRequestFactory requestFactory;
    private final ExecutorService proxiedExecutor;

    public LookupService(Reposilite reposilite) {
        this.reposilite = reposilite;
        this.frontend = reposilite.getFrontend();
        this.configuration = reposilite.getConfiguration();
        this.authenticator = reposilite.getAuthenticator();
        this.metadataService = reposilite.getMetadataService();
        this.repositoryService = reposilite.getRepositoryService();
        this.requestFactory = configuration.getProxied().isEmpty() ? null : new NetHttpTransport().createRequestFactory();
        this.proxiedExecutor = configuration.getProxied().isEmpty() ? null : Executors.newCachedThreadPool();
    }

    protected Result<Context, String> serveLocal(Context context) {
        Result<Pair<String[], Repository>, String> result = this.authenticator.authDefaultRepository(context, context.req.getRequestURI());

        if (result.containsError()) {
            return Result.error(result.getError());
        }

        String[] path = result.getValue().getKey();
        // remove repository name from path
        String[] requestPath = Arrays.copyOfRange(path, 1, path.length);

        // discard invalid requests (less than 'group/(artifact OR metadata)')
        if (requestPath.length < 2) {
            return Result.error("Missing artifact identifier");
        }

        Repository repository = result.getValue().getValue();
        String requestedFileName = requestPath[requestPath.length - 1];

        if (requestedFileName.equals("maven-metadata.xml")) {
            return metadataService
                    .generateMetadata(repository, requestPath)
                    .map(res -> context.contentType("text/xml").result(res));
        }

        // resolve requests for latest version of artifact
        if (requestedFileName.equalsIgnoreCase("latest")) {
            File requestDirectory = repository.getFile(requestPath).getParentFile();
            File[] versions = MetadataUtils.toSortedVersions(requestDirectory);
            File version = ArrayUtils.getFirst(versions);

            if (version == null) {
                return Result.error("Latest version not found");
            }

            return Result.ok(context.result(version.getName()));
        }

        // resolve snapshot requests
        if (requestedFileName.contains("-SNAPSHOT")) {
            repositoryService.resolveSnapshot(repository, requestPath);
            // update requested file name in case of snapshot request
            requestedFileName = requestPath[requestPath.length - 1];
        }

        Optional<Artifact> artifact = repository.find(requestPath);

        if (!artifact.isPresent()) {
            return Result.error("Artifact " + ContentJoiner.on("/").join(requestPath) + " not found");
        }

        File file = artifact.get().getFile(requestedFileName);
        FileInputStream content = null;

        try {
            // resolve content type associated with the requested extension
            String mimeType = Files.probeContentType(file.toPath());
            context.res.setContentType(mimeType != null ? mimeType : "application/octet-stream");

            // add content description to the header
            context.res.setContentLengthLong(file.length());
            context.res.setHeader("Content-Disposition", "attachment; filename=\"" + ArrayUtils.getLast(path) + "\"");

            // exclude content for head requests
            if (!context.method().equals("HEAD")) {
                content = new FileInputStream(file);
                IOUtils.copy(content, context.res.getOutputStream());
            }

            // success
            Reposilite.getLogger().info("RESOLVED " + file.getPath() + "; mime: " + mimeType + "; size: " + file.length());
            return Result.ok(context);
        } catch (Exception exception) {
            reposilite.throwException(context.req.getRequestURI(), exception);
            return Result.error("Cannot read artifact");
        } finally {
            FilesUtils.close(content);
        }
    }

    protected Result<CompletableFuture<Context>, String> serveProxied(Context context) {
        if (proxiedExecutor == null) {
            return Result.error("Proxied repositories are not enabled");
        }

        String uri = context.req.getRequestURI();

        if (StringUtils.countOccurrences(uri, "/") < 3) {
            return Result.error("Invalid proxied request");
        }

        return Result.ok(FutureUtils.submit(proxiedExecutor, future -> {
            for (String proxied : configuration.getProxied()) {
                try {
                    HttpRequest remoteRequest = requestFactory.buildGetRequest(new GenericUrl(proxied + uri));
                    remoteRequest.setThrowExceptionOnExecuteError(false);
                    remoteRequest.setConnectTimeout(3000);
                    remoteRequest.setReadTimeout(10000);
                    HttpResponse remoteResponse = remoteRequest.execute();

                    if (!remoteResponse.isSuccessStatusCode()) {
                        continue;
                    }

                    Long contentLength = remoteResponse.getHeaders().getContentLength();

                    if (contentLength != null && contentLength != 0) {
                        context.res.setContentLengthLong(contentLength);
                    }

                    if (!context.method().equals("HEAD")) {
                        IOUtils.copy(remoteResponse.getContent(), context.res.getOutputStream());
                    }

                    return future.complete(context
                            .status(remoteResponse.getStatusCode())
                            .contentType(remoteResponse.getContentType()));
                } catch (IOException e) {
                    Reposilite.getLogger().warn("Proxied repository " + proxied + " is unavailable: " + e.getMessage());
                } catch (Exception e) {
                    reposilite.throwException(uri, e);
                    future.cancel(true);
                }
            }

            return future.complete(context
                    .status(HttpStatus.SC_NOT_FOUND)
                    .contentType("text/html")
                    .result(frontend.forMessage("Artifact not found in local and remote repository")));
        }));
    }

}
