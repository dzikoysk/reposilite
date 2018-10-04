/*
 * Copyright (c) 2017 Dzikoysk
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

package org.panda_lang.nanomaven.workspace.repository;

import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.workspace.configuration.NanoMavenConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NanoRepositoryManager {

    private final Map<String, NanoRepository> repositories;

    public NanoRepositoryManager() {
        this.repositories = new HashMap<>(2);
    }

    public void scan(NanoMavenConfiguration configuration) {
        File rootDirectory = new File("repositories");
        repositories.clear();

        NanoMaven.getLogger().info("Scanning to find repositories...");

        for (String repositoryName : configuration.getRepositories()) {
            File repositoryDirectory = new File(rootDirectory, repositoryName);

            if (!repositoryDirectory.exists()) {
                NanoMaven.getLogger().warn("Nothing has been found!");
                return;
            }

            if (!repositoryDirectory.isDirectory()) {
                NanoMaven.getLogger().info("  Skipping " + repositoryDirectory.getName());
            }

            NanoRepository repository = new NanoRepository(repositoryName);
            NanoMaven.getLogger().info("  + " + repositoryDirectory.getName());

            repositories.put(repository.getRepositoryName(), repository);
        }

        NanoMaven.getLogger().info("Result: " + repositories.size() + " repositories have been found");
    }

    public NanoRepositoryProject find(String... path) {
        for (Map.Entry<String, NanoRepository> repositoryEntry : repositories.entrySet()) {
            NanoRepository repository = repositoryEntry.getValue();
            NanoRepositoryProject project = repository.get(path);

            if (project != null) {
                return project;
            }
        }

        return null;
    }

    public NanoRepository getRepository(String repositoryName) {
        return repositories.get(repositoryName);
    }

    public Collection<NanoRepository> getRepositories() {
        return repositories.values();
    }

}
