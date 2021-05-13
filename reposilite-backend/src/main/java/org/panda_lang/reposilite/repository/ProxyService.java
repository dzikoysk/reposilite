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
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.ReposiliteException;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.storage.StorageProvider;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Option;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ProxyService {

    private final boolean storeProxied;
    private final boolean proxyPrivate;
    private final int proxyConnectTimeout;
    private final int proxyReadTimeout;
    private final List<? extends String> proxied;
    private final RepositoryService repositoryService;
    private final FailureService failureService;
    private final HttpRequestFactory httpRequestFactory;
    private final StorageProvider storageProvider;

    public ProxyService(
            boolean storeProxied,
            boolean proxyPrivate,
            int proxyConnectTimeout,
            int proxyReadTimeout,
            List<? extends String> proxied,
            RepositoryService repositoryService,
            FailureService failureService,
            StorageProvider storageProvider) {

        this.storeProxied = storeProxied;
        this.proxyPrivate = proxyPrivate;
        this.proxyConnectTimeout = proxyConnectTimeout;
        this.proxyReadTimeout = proxyReadTimeout;
        this.proxied = proxied;
        this.repositoryService = repositoryService;
        this.storageProvider = storageProvider;
        this.failureService = failureService;
        this.httpRequestFactory = new NetHttpTransport().createRequestFactory();
    }

    protected Result<LookupResponse, ErrorDto> findProxied(ReposiliteContext context) {
        String uri = context.uri();
        Repository repository = repositoryService.getPrimaryRepository();

        // remove repository name if defined
        for (Repository localRepository : repositoryService.getRepositories()) {
            if (uri.startsWith("/" + localRepository.getName())) {
                repository = localRepository;
                uri = uri.substring(1 + localRepository.getName().length());
                break;
            }
        }

        if (!proxyPrivate && repository.isPrivate()) {
            return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "Proxying is disabled in private repositories"));
        }

        // /groupId/artifactId/<content>
        if (StringUtils.countOccurrences(uri, "/") < 3) {
            return Result.error(new ErrorDto(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Invalid proxied request"));
        }

        String remoteUri = uri;
        List<CompletableFuture<Void>> list = new ArrayList<>();
        List<HttpResponse> responses = Collections.synchronizedList(new ArrayList<>());

        for (String proxied : proxied) {
            list.add(CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest remoteRequest = httpRequestFactory.buildGetRequest(new GenericUrl(proxied + remoteUri));
                    remoteRequest.setThrowExceptionOnExecuteError(false);
                    remoteRequest.setConnectTimeout(proxyConnectTimeout * 1000);
                    remoteRequest.setReadTimeout(proxyReadTimeout * 1000);
                    HttpResponse remoteResponse = remoteRequest.execute();
                    HttpHeaders headers = remoteResponse.getHeaders();

                    if ("text/html".equals(headers.getContentType())) {
                        return;
                    }

                    if (remoteResponse.isSuccessStatusCode()) {
                        responses.add(remoteResponse);
                    }
                } catch (Exception exception) {
                    String message = "Proxied repository " + proxied + " is unavailable due to: " + exception.getMessage();
                    context.getLogger().error(message);

                    if (!(exception instanceof SocketTimeoutException)) {
                        failureService.throwException(remoteUri, new ReposiliteException(message, exception));
                    }
                }
            }));
        }

        CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();

        if (!responses.isEmpty()) {
            HttpResponse remoteResponse = responses.get(0);

            long contentLength = Option.of(remoteResponse.getHeaders().getContentLength()).orElseGet(0L);
            String[] path = remoteUri.split("/");

            FileDetailsDto fileDetails = new FileDetailsDto(FileDetailsDto.FILE, ArrayUtils.getLast(path), "", remoteResponse.getContentType(), contentLength);
            LookupResponse lookupResponse = new LookupResponse(fileDetails);

            if (!context.method().equals("HEAD")) {
                if (storeProxied) {
                    return store(remoteUri, remoteResponse, context).map(details -> lookupResponse);
                } else {
                    context.result(outputStream -> IOUtils.copyLarge(remoteResponse.getContent(), outputStream));
                }
            }

            return Result.ok(lookupResponse);
        } else {
            return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Artifact " + uri + " not found");
        }
    }

    private Result<FileDetailsDto, ErrorDto> store(String uri, HttpResponse remoteResponse, ReposiliteContext context) {
        if (storageProvider.isFull()) {
            String error = "Not enough storage space available for " + uri;
            context.getLogger().warn(error);
            return Result.error(new ErrorDto(HttpStatus.SC_INSUFFICIENT_STORAGE, error));
        }

        String repositoryName = StringUtils.split(uri.substring(1), "/")[0]; // skip first path separator
        Repository repository = repositoryService.getRepository(repositoryName);

        if (repository == null) {
            uri = repositoryService.getPrimaryRepository().getName() + uri;
        }

        Path proxiedFile = Paths.get(uri);

        try {
            Result<FileDetailsDto, ErrorDto> result = this.storageProvider.putFile(proxiedFile, remoteResponse.getContent());

            if (result.isOk()) {
                context.getLogger().info("Stored proxied " + proxiedFile + " from " + remoteResponse.getRequest().getUrl());
                context.result(output -> output.write(this.storageProvider.getFile(proxiedFile).get()));
            }

            return result;
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Cannot process artifact"));
        }
    }
}
