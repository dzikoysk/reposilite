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

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.ReposiliteContextFactory;
import org.panda_lang.reposilite.ReposiliteUtils;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.collection.Pair;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LookupApiEndpoint implements Handler {

    private final boolean rewritePathsEnabled;
    private final ReposiliteContextFactory contextFactory;
    private final RepositoryAuthenticator repositoryAuthenticator;
    private final RepositoryService repositoryService;

    public LookupApiEndpoint(
            boolean rewritePathsEnabled,
            ReposiliteContextFactory contextFactory,
            RepositoryAuthenticator repositoryAuthenticator,
            RepositoryService repositoryService) {

        this.rewritePathsEnabled = rewritePathsEnabled;
        this.contextFactory = contextFactory;
        this.repositoryAuthenticator = repositoryAuthenticator;
        this.repositoryService = repositoryService;
    }

    @OpenApi(
            operationId = "repositoryApi",
            summary = "Browse the contents of repositories using API",
            description = "Get details about the requested file as JSON response",
            tags = { "Repository" },
            pathParams = {
                    @OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true),
            },
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            description = "Returns document (different for directory and file) that describes requested resource",
                            content = {
                                    @OpenApiContent(from = FileDetailsDto.class),
                                    @OpenApiContent(from = FileListDto.class)
                            }
                    ),
                    @OpenApiResponse(
                            status = "401",
                            description = "Returns 401 in case of unauthorized attempt of access to private repository",
                            content = @OpenApiContent(from = ErrorDto.class)
                    ),
                    @OpenApiResponse(
                            status = "404",
                            description = "Returns 404 (for Maven) and frontend (for user) as a response if requested artifact is not in the repository"
                    ),
            }
    )
    @Override
    public void handle(@NotNull Context ctx) {
        ReposiliteContext context = contextFactory.create(ctx);
        Reposilite.getLogger().info("API " + context.uri() + " from " + context.address());

        String uri = ReposiliteUtils.normalizeUri(rewritePathsEnabled, repositoryService, StringUtils.replaceFirst(context.uri(), "/api", ""));

        if (StringUtils.isEmpty(uri) || "/".equals(uri)) {
            ctx.json(repositoryAuthenticator.findAvailableRepositories(context.headers()));
            return;
        }

        Result<Pair<String[], Repository>, ErrorDto> result = repositoryAuthenticator.authRepository(context.headers(), uri);

        if (result.isErr()) {
            ResponseUtils.errorResponse(ctx, result.getError().getStatus(), result.getError().getMessage());
            return;
        }

        Path requestedFile = repositoryService.getFile(uri);
        Optional<FileDetailsDto> latest = Optional.empty();

        try {
            latest = repositoryService.findLatest(requestedFile);
        } catch (IOException ignored) {

        }

        if (latest.isPresent()) {
            ctx.json(latest.get());
            return;
        }

        if (!Files.exists(requestedFile)) {
            ResponseUtils.errorResponse(ctx, HttpStatus.SC_NOT_FOUND, "File not found");
            return;
        }

        if (!Files.isDirectory(requestedFile)) {
            ctx.json(FileDetailsDto.of(requestedFile));
            return;
        }

        List<FileDetailsDto> list = new ArrayList<>();

        try {
            for (Path directory : Files.newDirectoryStream(requestedFile)) {
                list.add(FileDetailsDto.of(directory));
            }
        } catch (Exception ignored) {

        }

        ctx.json(new FileListDto(list));
    }

}
