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

import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.ReposiliteUtils;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.auth.Permission;
import org.panda_lang.reposilite.auth.Session;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.metadata.MetadataService;
import org.panda_lang.reposilite.storage.StorageProvider;
import org.panda_lang.utilities.commons.function.Result;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

public final class DeployService {

    private final boolean deployEnabled;
    private final boolean rewritePathsEnabled;
    private final Authenticator authenticator;
    private final RepositoryService repositoryService;
    private final MetadataService metadataService;
    private final StorageProvider storageProvider;

    public DeployService(
            boolean deployEnabled,
            boolean rewritePathsEnabled,
            Authenticator authenticator,
            RepositoryService repositoryService,
            MetadataService metadataService, StorageProvider storageProvider) {

        this.deployEnabled = deployEnabled;
        this.rewritePathsEnabled = rewritePathsEnabled;
        this.authenticator = authenticator;
        this.repositoryService = repositoryService;
        this.metadataService = metadataService;
        this.storageProvider = storageProvider;
    }

    public Result<CompletableFuture<Result<FileDetailsDto, ErrorDto>>, ErrorDto> deploy(ReposiliteContext context) {
        if (!deployEnabled) {
            return ResponseUtils.error(HttpStatus.SC_METHOD_NOT_ALLOWED, "Artifact deployment is disabled");
        }

        String uri = ReposiliteUtils.normalizeUri(rewritePathsEnabled, repositoryService, context.uri());
        Result<Session, String> authResult = this.authenticator.authByUri(context.headers(), uri);

        if (authResult.isErr()) {
            return ResponseUtils.error(HttpStatus.SC_UNAUTHORIZED, authResult.getError());
        }

        Session session = authResult.get();

        if (!session.hasPermission(Permission.WRITE) && !session.isManager()) {
            return ResponseUtils.error(HttpStatus.SC_UNAUTHORIZED, "Cannot deploy artifact without write permission");
        }

        if (storageProvider.isFull()) {
            return ResponseUtils.error(HttpStatus.SC_INSUFFICIENT_STORAGE, "Not enough storage space available");
        }

        Path path = repositoryService.getFile(uri);
        Result<FileDetailsDto, ErrorDto> fileDetails = storageProvider.getFileDetails(path);

        Path metadataFile = path.resolveSibling("maven-metadata.xml");
        metadataService.clearMetadata(metadataFile);

        Reposilite.getLogger().info("DEPLOY " + authResult.isOk() + " successfully deployed " + path + " from " + context.address());

        try {
            if (path.getFileName().toString().contains("maven-metadata")) {
                if (!storageProvider.exists(path)) {
                    ArrayList<String> list = new ArrayList<>();

                    path.forEach(p -> list.add(p.toString()));

                    String[] strarr = list.toArray(new String[0]);

                    // remove repository name from path
                    String[] requestPath = Arrays.copyOfRange(strarr, 1, strarr.length);

                    metadataService.generateMetadata(repositoryService.getRepository(path.getName(1).toString()), requestPath);

                    fileDetails = storageProvider.getFileDetails(path);
                }

                return Result.ok(CompletableFuture.completedFuture(Result.ok(fileDetails.get())));
            }

            return Result.ok(CompletableFuture.completedFuture(storageProvider.putFile(path, context.input())));
        } catch (Exception e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to upload artifact"));
        }
    }
}
