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

import org.panda_lang.reposilite.auth.Token;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.storage.StorageProvider;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class RepositoryService {

    private final RepositoryStorage repositoryStorage;
    private final StorageProvider storageProvider;

    public RepositoryService(
            Path workingDirectory,
            StorageProvider storageProvider) {

        this.repositoryStorage = new RepositoryStorage(workingDirectory, storageProvider);
        this.storageProvider = storageProvider;
    }

    public void load(Configuration configuration) throws IOException {
        repositoryStorage.load(configuration);
    }

    public void resolveSnapshot(Repository repository, String[] requestPath) {
        Path artifactFile = repository.getFile(requestPath);
        Path versionDirectory = artifactFile.getParent();

        Path[] builds = MetadataUtils.toSortedBuilds(storageProvider, versionDirectory).get();
        Path latestBuild = ArrayUtils.getFirst(builds);

        if (latestBuild == null) {
            return;
        }

        String version = StringUtils.replace(versionDirectory.getFileName().toString(), "-SNAPSHOT", StringUtils.EMPTY);
        Path artifactDirectory = versionDirectory.getParent();

        String identifier = MetadataUtils.toIdentifier(artifactDirectory.getFileName().toString(), version, latestBuild);
        requestPath[requestPath.length - 1] = StringUtils.replace(requestPath[requestPath.length - 1], "SNAPSHOT", identifier);
    }

    public Optional<FileDetailsDto> findLatest(Path requestedFile) throws IOException {
        if (requestedFile.getFileName().toString().equals("latest")) {
            Path parent = requestedFile.getParent();

            if (parent != null && storageProvider.isDirectory(parent)) {
                Result<Path[], ErrorDto> files = MetadataUtils.toSortedVersions(storageProvider, parent);

                if (files.isOk()) {
                    Path latest = ArrayUtils.getFirst(files.get());

                    if (latest != null) {
                        return Optional.ofNullable(storageProvider.getFileDetails(latest).orElseGet(e -> null));
                    }
                }
            }
        }

        return Optional.empty();
    }

    public List<Repository> getRepositories(Token token) {
        if (token.hasMultiaccess()) {
            return getRepositories();
        }

        for (Repository repository : getRepositories()) {
            String name = "/" + repository.getName();

            if (token.getPath().startsWith(name)) {
                return Collections.singletonList(repository);
            }
        }

        return Collections.emptyList();
    }

    public List<Repository> getRepositories() {
        return repositoryStorage.getRepositories();
    }

    public Repository getRepository(String repositoryName) {
        return repositoryStorage.getRepository(repositoryName);
    }

    public Repository getPrimaryRepository() {
        return repositoryStorage.getPrimaryRepository();
    }

    public Path getFile(String path) {
        return repositoryStorage.getFile(path);
    }

}
