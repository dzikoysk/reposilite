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
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class RepositoryStorage {

    protected static final long RETRY_WRITE_TIME = 1000L;

    private final Map<String, Repository> repositories = new LinkedHashMap<>(4);
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
        Path target = targetFile.toPath();
        FileUtils.forceMkdirParent(targetFile);

        try (FileChannel channel = FileChannel.open(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            FileLock lock = channel.lock();

            Files.copy(Objects.requireNonNull(source), target, StandardCopyOption.REPLACE_EXISTING);
            diskQuota.allocate(targetFile.length());

            lock.release();
        } catch (OverlappingFileLockException overlappingFileLockException) {
            Thread.sleep(RETRY_WRITE_TIME);
            storeFileSync(source, targetFile);
        }
    }

    File getFile(String path) {
        return new File(rootDirectory, path);
    }

    List<Repository> getRepositories() {
        return new ArrayList<>(repositories.values());
    }

    Repository getPrimaryRepository() {
        return primaryRepository;
    }

    Repository getRepository(String repositoryName) {
        return repositories.get(repositoryName);
    }

    DiskQuota getDiskQuota() {
        return diskQuota;
    }

    File getRootDirectory() {
        return rootDirectory;
    }

}
