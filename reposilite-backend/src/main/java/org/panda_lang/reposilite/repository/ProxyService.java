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
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.storage.StorageProvider;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Option;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ProxyService {

    private final boolean storeProxied;
    private final List<? extends String> proxied;
    private final RepositoryService repositoryService;
    private final HttpRequestFactory httpRequestFactory;
    private final StorageProvider storageProvider;

    public ProxyService(
            boolean storeProxied,
            List<? extends String> proxied,
            RepositoryService repositoryService, StorageProvider storageProvider) {

        this.storeProxied = storeProxied;
        this.proxied = proxied;
        this.repositoryService = repositoryService;
        this.storageProvider = storageProvider;
        this.httpRequestFactory = new NetHttpTransport().createRequestFactory();
    }

    protected Result<LookupResponse, ErrorDto> findProxied(ReposiliteContext context) {
        String uri = context.uri();

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
        List<CompletableFuture<Void>> list = new ArrayList<>();
        List<HttpResponse> responses = Collections.synchronizedList(new ArrayList<>());

        for (String proxied : proxied) {
            list.add(CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest remoteRequest = httpRequestFactory.buildGetRequest(new GenericUrl(proxied + remoteUri));
                    remoteRequest.setThrowExceptionOnExecuteError(false);
                    remoteRequest.setConnectTimeout(3000);
                    remoteRequest.setReadTimeout(10000);
                    HttpResponse remoteResponse = remoteRequest.execute();

                    if (remoteResponse.isSuccessStatusCode()) {
                        responses.add(remoteResponse);
                    }
                } catch (Exception exception) {
                    String message = "Proxied repository " + proxied + " is unavailable due to: " + exception;
                    Reposilite.getLogger().error(message);
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
            Reposilite.getLogger().warn(error);
            return Result.error(new ErrorDto(HttpStatus.SC_INSUFFICIENT_STORAGE, error));
        }

        String repositoryName = StringUtils.split(uri.substring(1), "/")[0]; // skip first path separator
        Repository repository = repositoryService.getRepository(repositoryName);

        if (repository == null) {
            uri = repositoryService.getPrimaryRepository().getName() + uri;
        }

        Path proxiedFile = repositoryService.getFile(uri);

        try {
            Result<FileDetailsDto, ErrorDto> result = this.storageProvider.putFile(proxiedFile, remoteResponse.getContent());

            if (result.isOk()) {
                Reposilite.getLogger().info("Stored proxied " + proxiedFile + " from " + remoteResponse.getRequest().getUrl());
                context.result(output -> output.write(this.storageProvider.getFile(proxiedFile).get()));
            }

            return result;
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Cannot process artifact"));
        }
    }
}
