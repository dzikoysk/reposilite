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
import org.panda_lang.utilities.commons.function.Result;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public final class DeployService {

    private final boolean deployEnabled;
    private final boolean rewritePathsEnabled;
    private final Authenticator authenticator;
    private final RepositoryService repositoryService;
    private final MetadataService metadataService;

    public DeployService(
            boolean deployEnabled,
            boolean rewritePathsEnabled,
            Authenticator authenticator,
            RepositoryService repositoryService,
            MetadataService metadataService) {

        this.deployEnabled = deployEnabled;
        this.rewritePathsEnabled = rewritePathsEnabled;
        this.authenticator = authenticator;
        this.repositoryService = repositoryService;
        this.metadataService = metadataService;
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

        if (!repositoryService.getDiskQuota().hasUsableSpace()) {
            return ResponseUtils.error(HttpStatus.SC_INSUFFICIENT_STORAGE, "Out of disk space");
        }

        File file = repositoryService.getFile(uri);
        FileDetailsDto fileDetails = FileDetailsDto.of(file);

        File metadataFile = new File(file.getParentFile(), "maven-metadata.xml");
        metadataService.clearMetadata(metadataFile);

        Reposilite.getLogger().info("DEPLOY " + authResult.get().getAlias() + " successfully deployed " + file + " from " + context.address());

        if (file.getName().contains("maven-metadata")) {
            return Result.ok(CompletableFuture.completedFuture(Result.ok(fileDetails)));
        }

        CompletableFuture<Result<FileDetailsDto, ErrorDto>> task = repositoryService.storeFile(
                uri,
                file,
                context::input,
                () -> fileDetails,
                exception -> new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to upload artifact"));

        return Result.ok(task);
    }

}
