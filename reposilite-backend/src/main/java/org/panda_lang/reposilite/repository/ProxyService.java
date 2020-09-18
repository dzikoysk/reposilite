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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

final class ProxyService {

    private final boolean storeProxied;
    private final boolean rewritePathsEnabled;
    private final List<? extends String> proxied;
    private final ExecutorService executorService;
    private final RepositoryService repositoryService;
    private final FailureService failureService;
    private final HttpRequestFactory httpRequestFactory;

    ProxyService(
            boolean storeProxied,
            boolean rewritePathsEnabled,
            List<? extends String> proxied,
            ExecutorService executorService,
            FailureService failureService,
            RepositoryService repositoryService) {

        this.storeProxied = storeProxied;
        this.rewritePathsEnabled = rewritePathsEnabled;
        this.proxied = proxied;
        this.executorService = executorService;
        this.repositoryService = repositoryService;
        this.failureService = failureService;
        this.httpRequestFactory = new NetHttpTransport().createRequestFactory();
    }

    protected Result<CompletableFuture<Result<Context, ErrorDto>>, ErrorDto> findProxied(Context context) {
        String uri = context.req.getRequestURI();

        // remove repository name if defined
        for (Repository repository : repositoryService.getRepositories()) {
            if (uri.startsWith("/" + repository.getName())) {
                uri = uri.substring(1 + repository.getName().length());
                break;
            }
        }

        // /groupId/artifactId/<content>
        if (StringUtils.countOccurrences(uri, "/") < 3) {
            return Result.error(new ErrorDto(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Invalid proxied request"));
        }

        String remoteUri = uri;
        CompletableFuture<Result<Context, ErrorDto>> proxiedResult = new CompletableFuture<>();

        executorService.submit(() -> {
            for (String proxied : proxied) {
                try {
                    HttpRequest remoteRequest = httpRequestFactory.buildGetRequest(new GenericUrl(proxied + remoteUri));
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
                        if (storeProxied) {
                            store(context, remoteUri, remoteResponse);
                        }
                        else {
                            IOUtils.copy(remoteResponse.getContent(), context.res.getOutputStream());
                        }
                    }

                    if (remoteResponse.getContentType() != null) {
                        context.contentType(remoteResponse.getContentType());
                    }

                    proxiedResult.complete(Result.ok(context.status(remoteResponse.getStatusCode())));
                    return;
                } catch (Exception exception) {
                    Reposilite.getLogger().warn("Proxied repository " + proxied + " is unavailable: " + exception.getMessage());
                    failureService.throwException(remoteUri, exception);
                }
            }

            proxiedResult.complete(ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Artifact not found in local and remote repository"));
        });

        return Result.ok(proxiedResult);
    }

    private void store(Context context, String uri, HttpResponse remoteResponse) throws IOException {
        DiskQuota diskQuota = repositoryService.getDiskQuota();

        if (!diskQuota.hasUsableSpace()) {
            Reposilite.getLogger().warn("Out of disk space - Cannot store proxied artifact " + uri);
            return;
        }

        String repositoryName = StringUtils.split(uri, "/")[1]; // skip first path separator
        Repository repository = repositoryService.getRepository(repositoryName);

        if (repository == null) {
            if (!rewritePathsEnabled) {
                return;
            }

            uri = repositoryService.getPrimaryRepository().getName() + uri;
        }

        File proxiedFile = repositoryService.getFile(uri);
        FileUtils.copyInputStreamToFile(remoteResponse.getContent(), proxiedFile);
        FileUtils.copyFile(proxiedFile, context.res.getOutputStream());
        diskQuota.allocate(proxiedFile.length());

        Reposilite.getLogger().info("Stored proxied " + uri);
    }

}
