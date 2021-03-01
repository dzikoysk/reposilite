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
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.*;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.collection.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LookupApiEndpoint implements RepositoryController {

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

    @Override
    public Context handleContext(Context ctx) {
        ReposiliteContext context = contextFactory.create(ctx);
        Reposilite.getLogger().info("API " + context.uri() + " from " + context.address());

        String uri = ReposiliteUtils.normalizeUri(rewritePathsEnabled, repositoryService, StringUtils.replaceFirst(context.uri(), "/api", ""));

        if (StringUtils.isEmpty(uri) || "/".equals(uri)) {
            return ctx.json(repositoryAuthenticator.findAvailableRepositories(context.headers()));
        }

        Result<Pair<String[], Repository>, ErrorDto> result = repositoryAuthenticator.authRepository(context.headers(), uri);

        if (result.containsError()) {
            return ResponseUtils.errorResponse(ctx, result.getError().getStatus(), result.getError().getMessage());
        }

        Path requestedFile = repositoryService.getFile(uri);
        Optional<FileDetailsDto> latest = Optional.empty();

        try {
            latest = repositoryService.findLatest(requestedFile);
        } catch (IOException ignored) {

        }

        if (latest.isPresent()) {
            return ctx.json(latest.get());
        }

        if (!Files.exists(requestedFile)) {
            return ResponseUtils.errorResponse(ctx, HttpStatus.SC_NOT_FOUND, "File not found");
        }

        if (!Files.isDirectory(requestedFile)) {
            return ctx.json(FileDetailsDto.of(requestedFile));
        }

        List<FileDetailsDto> list = new ArrayList<>();

        try {
            for (Path directory : Files.newDirectoryStream(requestedFile)) {
                list.add(FileDetailsDto.of(directory));
            }
        } catch (Exception ignored) {

        }

        return ctx.json(new FileListDto(list));
    }

}
