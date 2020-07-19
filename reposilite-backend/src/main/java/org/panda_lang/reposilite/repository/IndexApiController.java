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
import io.vavr.collection.Stream;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.RepositoryController;
import org.panda_lang.reposilite.api.ErrorDto;
import org.panda_lang.reposilite.api.ErrorUtils;
import org.panda_lang.reposilite.auth.Authenticator;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.collection.Pair;

import java.io.File;
import java.util.Optional;
import java.util.stream.Collectors;

public final class IndexApiController implements RepositoryController {

    private final Configuration configuration;
    private final Authenticator authenticator;
    private final RepositoryService repositoryService;

    public IndexApiController(Reposilite reposilite) {
        this.configuration = reposilite.getConfiguration();
        this.authenticator = reposilite.getAuthenticator();
        this.repositoryService = reposilite.getRepositoryService();
    }

    @Override
    public Context handleContext(Context context) {
        Reposilite.getLogger().info("API " + context.req.getRequestURI() + " from " + context.ip());
        String uri = RepositoryUtils.normalizeUri(configuration, repositoryService, StringUtils.replaceFirst(context.req.getRequestURI(), "/api", ""));

        if (StringUtils.isEmpty(uri) || "/".equals(uri)) {
            return context.json(listRepositories(context));
        }

        Result<Pair<String[], Repository>, ErrorDto> result = authenticator.authRepository(context, uri);

        if (result.containsError()) {
            return ErrorUtils.error(context, HttpStatus.SC_UNAUTHORIZED, "Unauthorized request");
        }

        File requestedFile = repositoryService.getFile(uri);
        Optional<FileDto> latest = findLatest(requestedFile);

        if (latest.isPresent()) {
            return context.json(latest.get());
        }

        if (!requestedFile.exists()) {
            return ErrorUtils.error(context, HttpStatus.SC_NOT_FOUND, "File not found");
        }

        if (requestedFile.isFile()) {
            return context.json(FileDto.of(requestedFile));
        }

        return context.json(new FileListDto(Stream.of(FilesUtils.listFiles(requestedFile))
                .map(FileDto::of)
                .transform(stream -> MetadataUtils.toSorted(stream, FileDto::getName, FileDto::isDirectory))
                .toJavaList()));
    }

    private FileListDto listRepositories(Context context) {
        return new FileListDto(repositoryService.getRepositories().stream()
                .filter(repository -> repository.isPublic() || authenticator.authUri(context, repository.getUri()).isDefined())
                .map(Repository::getFile)
                .map(FileDto::of)
                .collect(Collectors.toList()));
    }

    private Optional<FileDto> findLatest(File requestedFile) {
        if (requestedFile.getName().equals("latest")) {
            File parent = requestedFile.getParentFile();

            if (parent != null && parent.exists()) {
                File[] files = MetadataUtils.toSortedVersions(parent);
                File latest = ArrayUtils.getFirst(files);

                if (latest != null) {
                    return Optional.of(FileDto.of(latest));
                }
            }

        }

        return Optional.empty();
    }

}
