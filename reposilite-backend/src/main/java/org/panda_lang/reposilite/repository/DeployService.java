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
import org.panda_lang.utilities.commons.collection.Pair;
import org.panda_lang.utilities.commons.function.Option;
import org.panda_lang.utilities.commons.function.Result;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class DeployService {

    private final boolean rewritePathsEnabled;
    private final Authenticator authenticator;
    private final RepositoryService repositoryService;
    private final MetadataService metadataService;

    public DeployService(
            boolean rewritePathsEnabled,
            Authenticator authenticator,
            RepositoryService repositoryService,
            MetadataService metadataService) {

        this.rewritePathsEnabled = rewritePathsEnabled;
        this.authenticator = authenticator;
        this.repositoryService = repositoryService;
        this.metadataService = metadataService;
    }

    public Result<FileDetailsDto, ErrorDto> deploy(ReposiliteContext context) {
        Option<String> uriValue = ReposiliteUtils.normalizeUri(context.uri());

        if (uriValue.isEmpty()) {
            return ResponseUtils.error(HttpStatus.SC_BAD_REQUEST, "Invalid GAV path");
        }

        String uri = uriValue.get();

        Result<Session, String> authResult = this.authenticator.authByUri(context.headers(), uri);

        if (authResult.isErr()) {
            return ResponseUtils.error(HttpStatus.SC_UNAUTHORIZED, authResult.getError());
        }

        Session session = authResult.get();

        if (!session.hasPermission(Permission.WRITE) && !session.isManager()) {
            return ResponseUtils.error(HttpStatus.SC_UNAUTHORIZED, "Cannot deploy artifact without write permission");
        }

        Option<Repository> repositoryValue = ReposiliteUtils.getRepository(rewritePathsEnabled, repositoryService, uri);

        if (repositoryValue.isEmpty()) {
            return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Repository not found");
        }

        Repository repository = repositoryValue.get();

        if (!repository.isDeployEnabled()) {
            return ResponseUtils.error(HttpStatus.SC_METHOD_NOT_ALLOWED, "Artifact deployment is disabled");
        }

        if (repository.isFull()) {
            return ResponseUtils.error(HttpStatus.SC_INSUFFICIENT_STORAGE, "Not enough storage space available");
        }

        Path path = Paths.get(uri);

        Path metadataFile = path.resolveSibling("maven-metadata.xml");
        metadataService.clearMetadata(metadataFile);

        try {
            Result<FileDetailsDto, ErrorDto> result;

            if (path.getFileName().toString().contains("maven-metadata")) {
                result = metadataService.getMetadata(repository, metadataFile).map(Pair::getKey);
            } else {
                result = repository.putFile(path, context.input());
            }

            if (result.isOk()) {
                Reposilite.getLogger().info("DEPLOY " + authResult.isOk() + " successfully deployed " + path + " from " + context.address());
            }

            return result;
        } catch (Exception e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to upload artifact"));
        }
    }
}
