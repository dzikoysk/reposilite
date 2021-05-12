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

import org.jetbrains.annotations.Nullable;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.auth.Token;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.config.Configuration.RepositoryConfiguration;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.storage.StorageProviderFactory;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

public final class RepositoryService {

    private final Map<String, Repository> repositories = new LinkedHashMap<>(4);
    private Repository primaryRepository;

    public void load(Configuration configuration) {
        Reposilite.getLogger().info("--- Loading repositories");
        StorageProviderFactory storageProviderFactory = new StorageProviderFactory();

        for (Entry<String, RepositoryConfiguration> repositoryEntry : configuration.repositories.entrySet()) {
            String repositoryName = repositoryEntry.getKey();
            RepositoryConfiguration repositoryConfiguration = repositoryEntry.getValue();

            Repository repository = new Repository(
                    repositoryName,
                    RepositoryVisibility.valueOf(repositoryConfiguration.visibility.toUpperCase()),
                    storageProviderFactory.createStorageProvider(repositoryName, repositoryConfiguration.storageProvider),
                    repositoryConfiguration.deployEnabled
            );

            repositories.put(repository.getName(), repository);
            boolean primary = primaryRepository == null;

            if (primary) {
                this.primaryRepository = repository;
            }

            Reposilite.getLogger().info("+ " + repositoryName + (repository.isPrivate() ? " (private)" : "") + (primary ? " (primary)" : ""));
        }

        Reposilite.getLogger().info(repositories.size() + " repositories have been found");
    }

    public @Nullable Path resolveSnapshot(Repository repository, Path requestPath) {
        Path artifactFile = repository.relativize(requestPath);
        Path versionDirectory = artifactFile.getParent();

        Path[] builds = MetadataUtils.toSortedBuilds(repository, versionDirectory).get();
        Path latestBuild = ArrayUtils.getFirst(builds);

        if (latestBuild == null) {
            return null;
        }

        String version = StringUtils.replace(versionDirectory.getFileName().toString(), "-SNAPSHOT", StringUtils.EMPTY);
        Path artifactDirectory = versionDirectory.getParent();

        String identifier = MetadataUtils.toIdentifier(artifactDirectory.getFileName().toString(), version, latestBuild);
        return requestPath.getParent().resolve(requestPath.getFileName().toString().replace("SNAPSHOT", identifier));
    }

    public Optional<FileDetailsDto> findLatest(Path requestedFile) throws IOException {
        if (requestedFile.getFileName().toString().equals("latest")) {
            Path parent = requestedFile.getParent();

            for (Repository repository : repositories.values()) {
                if (parent != null && repository.isDirectory(parent)) {
                    Result<Path[], ErrorDto> files = MetadataUtils.toSortedVersions(repository, parent);

                    if (files.isOk()) {
                        Path latest = ArrayUtils.getFirst(files.get());

                        if (latest != null) {
                            return Optional.ofNullable(repository.getFileDetails(latest).orElseGet(e -> null));
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    public Collection<Repository> getRepositories(Token token) {
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

    public Collection<Repository> getRepositories() {
        return this.repositories.values();
    }

    public Repository getRepository(String repositoryName) {
        return this.repositories.get(repositoryName);
    }

    public Repository getPrimaryRepository() {
        return this.primaryRepository;
    }

}
