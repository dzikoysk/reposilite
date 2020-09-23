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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class RepositoryStorage {

    private final Map<String, Repository> repositories = new LinkedHashMap<>(4);
    private final Map<String, RepositoryFile> locks = new ConcurrentHashMap<>();

    private final File rootDirectory;
    private final DiskQuota diskQuota;
    private Repository primaryRepository;

    RepositoryStorage(File rootDirectory, String diskQuota) {
        this.rootDirectory = rootDirectory;
        this.diskQuota = DiskQuota.of(getRootDirectory().getParentFile(), diskQuota);
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

    void storeFileSync(InputStream source, File targetFile) throws Exception {
        RepositoryFile repositoryFile = new RepositoryFile(this, targetFile);
        repositoryFile.store(source);
    }

    synchronized boolean lock(RepositoryFile file) {
        if (!locks.containsKey(file.getIdentifier())) {
            locks.put(file.getIdentifier(), file);
            return true;
        }

        return false;
    }

    synchronized boolean unlock(RepositoryFile file) {
        RepositoryFile locked = locks.get(file.getIdentifier());

        if (file.equals(locked)) {
            locks.remove(file.getIdentifier());
            return true;
        }

        return false;
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
