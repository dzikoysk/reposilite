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
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteUtils;
import org.panda_lang.reposilite.RepositoryController;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.collection.Pair;
import org.panda_lang.utilities.commons.function.PandaStream;

import java.io.File;
import java.util.Optional;

public final class LookupApiController implements RepositoryController {

    private final boolean rewritePathsEnabled;
    private final RepositoryAuthenticator repositoryAuthenticator;
    private final RepositoryService repositoryService;
    private final LookupService lookupService;

    public LookupApiController(
            boolean rewritePathsEnabled,
            RepositoryAuthenticator repositoryAuthenticator,
            RepositoryService repositoryService,
            LookupService lookupService) {

        this.rewritePathsEnabled = rewritePathsEnabled;
        this.repositoryAuthenticator = repositoryAuthenticator;
        this.repositoryService = repositoryService;
        this.lookupService = lookupService;
    }

    @Override
    public Context handleContext(Context context) {
        Reposilite.getLogger().info("API " + context.req.getRequestURI() + " from " + context.ip());
        String uri = ReposiliteUtils.normalizeUri(rewritePathsEnabled, repositoryService, StringUtils.replaceFirst(context.req.getRequestURI(), "/api", ""));

        if (StringUtils.isEmpty(uri) || "/".equals(uri)) {
            return context.json(lookupService.findAvailableRepositories(context.headerMap()));
        }

        Result<Pair<String[], Repository>, ErrorDto> result = repositoryAuthenticator.authRepository(context.headerMap(), context.req.getRequestURI(), uri);

        if (result.containsError()) {
            return ResponseUtils.errorResponse(context, result.getError().getStatus(), result.getError().getMessage());
        }

        File requestedFile = repositoryService.getFile(uri);
        Optional<FileDetailsDto> latest = lookupService.findLatest(requestedFile);

        if (latest.isPresent()) {
            return context.json(latest.get());
        }

        if (!requestedFile.exists()) {
            return ResponseUtils.errorResponse(context, HttpStatus.SC_NOT_FOUND, "File not found");
        }

        if (requestedFile.isFile()) {
            return context.json(FileDetailsDto.of(requestedFile));
        }

        return context.json(new FileListDto(PandaStream.of(FilesUtils.listFiles(requestedFile))
                .map(FileDetailsDto::of)
                .transform(stream -> MetadataUtils.toSorted(stream, FileDetailsDto::getName, FileDetailsDto::isDirectory))
                .toList()));
    }

}
