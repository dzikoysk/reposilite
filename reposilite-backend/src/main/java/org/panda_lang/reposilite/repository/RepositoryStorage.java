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

import org.apache.commons.io.FileUtils;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.config.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class RepositoryStorage {

    private static final long RETRY_WRITE_TIME = 2000L;

    private final Map<String, Repository> repositories = new LinkedHashMap<>(4);

    private final File rootDirectory;
    private final DiskQuota diskQuota;
    private final ExecutorService ioService;
    private final ScheduledExecutorService retryService;
    private Repository primaryRepository;

    RepositoryStorage(File rootDirectory, String diskQuota, ExecutorService ioService, ScheduledExecutorService retryService) {
        this.rootDirectory = rootDirectory;
        this.diskQuota = DiskQuota.of(getRootDirectory().getParentFile(), diskQuota);
        this.ioService = ioService;
        this.retryService = retryService;
    }

    void load(Configuration configuration) {
        Reposilite.getLogger().info("--- Loading repositories");

        if (rootDirectory.mkdirs()) {
            Reposilite.getLogger().info("Default repository directory has been created");
        }
        else {
            Reposilite.getLogger().info("Using an existing repository directory");
        }

        for (String repositoryName : configuration.repositories) {
            boolean hidden = repositoryName.startsWith(".");
            boolean primary = primaryRepository == null;

            if (hidden) {
                repositoryName = repositoryName.substring(1);
            }

            File repositoryDirectory = new File(rootDirectory, repositoryName);

            if (repositoryDirectory.mkdirs()) {
                Reposilite.getLogger().info("+ Repository '" + repositoryName + "' has been created");
            }

            Repository repository = new Repository(rootDirectory, repositoryName, hidden);
            repositories.put(repository.getName(), repository);

            if (primary) {
                this.primaryRepository = repository;
            }

            Reposilite.getLogger().info("+ " + repositoryDirectory.getName() + (hidden ? " (hidden)" : "") + (primary ? " (primary)" : ""));
        }

        Reposilite.getLogger().info(repositories.size() + " repositories have been found");
    }

    CompletableFuture<File> storeFile(InputStream source, File targetFile) throws Exception {
        return storeFile(new CompletableFuture<>(), source, targetFile);
    }

    private CompletableFuture<File> storeFile(CompletableFuture<File> task, InputStream source, File targetFile) throws IOException {
        File lockedFile = new File(targetFile.getAbsolutePath() + ".lock");

        if (lockedFile.exists()) {
            retryService.schedule(() -> {
                ioService.submit(() -> {
                    storeFile(task, source, targetFile);
                    return null;
                });
            }, RETRY_WRITE_TIME, TimeUnit.MILLISECONDS);

            return task;
        }

        FileUtils.forceMkdirParent(targetFile);

        if (targetFile.exists()) {
            Files.move(targetFile.toPath(), lockedFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }

        Files.copy(source, lockedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        diskQuota.allocate(lockedFile.length());
        Files.move(lockedFile.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        task.complete(targetFile);
        return task;
    }

    File getFile(String path) {
        return new File(rootDirectory, path);
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

    DiskQuota getDiskQuota() {
        return diskQuota;
    }

    File getRootDirectory() {
        return rootDirectory;
    }

}
