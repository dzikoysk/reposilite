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
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.ThrowingFunction;
import org.panda_lang.utilities.commons.function.ThrowingSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class RepositoryService {

    private final RepositoryStorage repositoryStorage;
    private final ExecutorService ioExecutorService;
    private final FailureService failureService;

    public RepositoryService(String workingDirectory, String diskQuota, ExecutorService ioExecutorService, FailureService failureService) {
        this.repositoryStorage = new RepositoryStorage(new File(workingDirectory, "repositories"), diskQuota);
        this.ioExecutorService = ioExecutorService;
        this.failureService = failureService;
    }

    public void load(Configuration configuration) {
        repositoryStorage.load(configuration);
    }

    public <R, E, T extends Exception> CompletableFuture<Result<R, E>> storeFile(
            String id,
            File targetFile,
            ThrowingSupplier<InputStream, IOException> source,
            ThrowingSupplier<R, T> onSuccess,
            ThrowingFunction<Exception, E, T> onError) {

        CompletableFuture<Result<R, E>> task = new CompletableFuture<>();

        ioExecutorService.submit(() -> {
            try {
                repositoryStorage.storeFileSync(source.get(), targetFile);
                return task.complete(Result.ok(onSuccess.get()));
            } catch (Exception exception) {
                failureService.throwException(id, exception);
                return task.complete(Result.error(onError.apply(exception)));
            }
        });

        return task;
    }

    public String[] resolveSnapshot(Repository repository, String[] requestPath) {
        File artifactFile = repository.getFile(requestPath);
        File versionDirectory = artifactFile.getParentFile();

        File[] builds = MetadataUtils.toSortedBuilds(versionDirectory);
        File latestBuild = ArrayUtils.getFirst(builds);

        if (latestBuild == null) {
            return requestPath;
        }

        String version = StringUtils.replace(versionDirectory.getName(), "-SNAPSHOT", StringUtils.EMPTY);
        File artifactDirectory = versionDirectory.getParentFile();

        String identifier = MetadataUtils.toIdentifier(artifactDirectory.getName(), version, latestBuild);
        requestPath[requestPath.length - 1] = StringUtils.replace(requestPath[requestPath.length - 1], "SNAPSHOT", identifier);

        return requestPath;
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

    public File getFile(String path) {
        return repositoryStorage.getFile(path);
    }

    public DiskQuota getDiskQuota() {
        return repositoryStorage.getDiskQuota();
    }

}
