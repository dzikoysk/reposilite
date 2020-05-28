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

import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RepositoryService {

    private final File rootDirectory = new File("repositories");
    private final Map<String, Repository> repositories;

    public RepositoryService() {
        this.repositories = new LinkedHashMap<>(2);
    }

    public void load(Configuration configuration) {
        Reposilite.getLogger().info("--- Loading repository");

        if (rootDirectory.mkdirs()) {
            Reposilite.getLogger().info("Default repository directory has been created");
        }
        else {
            Reposilite.getLogger().info("Using an existing repository directory");
        }

        for (String repository : configuration.getRepositories()) {
            File repositoryDirectory = new File(rootDirectory, repository);

            if (repositoryDirectory.mkdirs()) {
                Reposilite.getLogger().info("+ Repository '" + repository + "' has been created");
            }
        }

        Reposilite.getLogger().info("");
    }

    public void scan(Configuration configuration) {
        Reposilite.getLogger().info("--- Scanning to find repositories");

        for (String repositoryName : configuration.getRepositories()) {
            File repositoryDirectory = new File(rootDirectory, repositoryName);

            if (!repositoryDirectory.exists()) {
                Reposilite.getLogger().warn("Nothing has been found!");
                return;
            }

            if (!repositoryDirectory.isDirectory()) {
                Reposilite.getLogger().info("  Skipping " + repositoryDirectory.getName());
            }

            Repository repository = new Repository(repositoryName);
            Reposilite.getLogger().info("+ " + repositoryDirectory.getName());

            repositories.put(repository.getRepositoryName(), repository);
        }

        Reposilite.getLogger().info(repositories.size() + " repositories have been found");
    }

    public String[] resolveSnapshot(Repository repository, String[] requestPath) {
        File artifactFile = RepositoryUtils.toRequestedFile(repository, requestPath);
        File versionDirectory = artifactFile.getParentFile();

        File[] builds = MetadataUtils.toSortedBuilds(versionDirectory);
        File latestBuild = MetadataUtils.getLatest(builds);

        if (latestBuild == null) {
            return requestPath;
        }

        String version = StringUtils.replace(versionDirectory.getName(), "-SNAPSHOT", StringUtils.EMPTY);
        File artifactDirectory = versionDirectory.getParentFile();

        String identifier = MetadataUtils.toIdentifier(artifactDirectory.getName(), version, latestBuild);
        requestPath[requestPath.length - 1] = StringUtils.replace(requestPath[requestPath.length - 1], "SNAPSHOT", identifier);

        return requestPath;
    }

    public File getFile(String path) {
        return new File(rootDirectory, path);
    }

    public Repository getRepository(String repositoryName) {
        return repositories.get(repositoryName);
    }

    public Collection<Repository> getRepositories() {
        return repositories.values();
    }

}
