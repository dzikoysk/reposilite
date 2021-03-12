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

import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.storage.StorageProvider;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class RepositoryStorage {
    private final Map<String, Repository> repositories = new LinkedHashMap<>(4);

    private final StorageProvider storageProvider;
    private final Path rootDirectory;
    private Repository primaryRepository;

    RepositoryStorage(Path workingDirectory, StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        this.rootDirectory = workingDirectory.resolve("repositories");
    }

    void load(Configuration configuration) {
        Reposilite.getLogger().info("--- Loading repositories");

        for (String repositoryName : configuration.repositories) {
            boolean hidden = repositoryName.startsWith(".");
            boolean primary = primaryRepository == null;

            if (hidden) {
                repositoryName = repositoryName.substring(1);
            }

            Path repositoryDirectory = rootDirectory.resolve(repositoryName);
            Repository repository = new Repository(rootDirectory, repositoryName, hidden, storageProvider);
            repositories.put(repository.getName(), repository);

            if (primary) {
                this.primaryRepository = repository;
            }

            Reposilite.getLogger().info("+ " + repositoryDirectory.getFileName() + (hidden ? " (hidden)" : "") + (primary ? " (primary)" : ""));
        }

        Reposilite.getLogger().info(repositories.size() + " repositories have been found");
    }

    CompletableFuture<Path> storeFile(InputStream source, Path targetFile) {
        return storeFile(new CompletableFuture<>(), source, targetFile);
    }

    private CompletableFuture<Path> storeFile(CompletableFuture<Path> task, InputStream source, Path targetFile) {
        this.storageProvider.putFile(targetFile, source);
        task.complete(targetFile);
        return task;
    }

    Path getFile(String path) {
        return rootDirectory.resolve(path);
    }

    Repository getRepository(String repositoryName) {
        return repositories.get(repositoryName);
    }

    List<Repository> getRepositories() {
        return new ArrayList<>(repositories.values());
    }

    Repository getPrimaryRepository() {
        return primaryRepository;
    }

    Path getRootDirectory() {
        return rootDirectory;
    }

    StorageProvider getStorageProvider() {
        return storageProvider;
    }

}
