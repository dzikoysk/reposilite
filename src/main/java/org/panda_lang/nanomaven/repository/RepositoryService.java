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

package org.panda_lang.nanomaven.repository;

import org.panda_lang.nanomaven.NanoConfiguration;
import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.repository.Metadata.Versioning;
import org.panda_lang.nanomaven.repository.Metadata.Versioning.Versions;
import org.panda_lang.utilities.commons.FileUtils;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepositoryService {

    private final Map<String, Repository> repositories;
    private final Map<String, String> metadataCache;

    public RepositoryService() {
        this.repositories = new LinkedHashMap<>(2);
        this.metadataCache = new HashMap<>();
    }

    public void scan(NanoConfiguration configuration) {
        File rootDirectory = new File("repositories");
        repositories.clear();

        NanoMaven.getLogger().info("--- Scanning to find repositories");

        for (String repositoryName : configuration.getRepositories()) {
            File repositoryDirectory = new File(rootDirectory, repositoryName);

            if (!repositoryDirectory.exists()) {
                NanoMaven.getLogger().warn("Nothing has been found!");
                return;
            }

            if (!repositoryDirectory.isDirectory()) {
                NanoMaven.getLogger().info("  Skipping " + repositoryDirectory.getName());
            }

            Repository repository = new Repository(repositoryName);
            NanoMaven.getLogger().info("+ " + repositoryDirectory.getName());

            repositories.put(repository.getRepositoryName(), repository);
        }

        NanoMaven.getLogger().info(repositories.size() + " repositories have been found");
    }

    public String generateMetadata(Repository repository, String[] path) throws IOException {
        File metadataFile = new File(repository.getLocalPath() + File.separator + ContentJoiner.on(File.separator).join(path));
        File artifactDirectory = metadataFile.getParentFile();
        File[] list = RepositoryUtils.toSortedDirectories(artifactDirectory);
        File latest = RepositoryUtils.getLatest(list);

        if (latest == null) {
            return "Empty metadata";
        }

        Versions versions = new Versions(Stream.of(list).map(File::getName).collect(Collectors.toList()));
        Versioning versioning = new Versioning(latest.getName(), latest.getName(), versions, latest.lastModified());
        Metadata metadata = new Metadata(ContentJoiner.on(".").join(Arrays.copyOfRange(path, 0, path.length - 1)).toString(), artifactDirectory.getName(), versioning);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Metadata.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(metadata, metadataFile);
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return FileUtils.getContentOfFile(metadataFile);
    }

    public Repository getRepository(String repositoryName) {
        return repositories.get(repositoryName);
    }

    public Collection<Repository> getRepositories() {
        return repositories.values();
    }

}
