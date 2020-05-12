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

package org.panda_lang.nanomaven.metadata;

import org.panda_lang.nanomaven.NanoMaven;
import org.panda_lang.nanomaven.repository.Repository;
import org.panda_lang.nanomaven.repository.RepositoryUtils;
import org.panda_lang.nanomaven.utils.FilesUtils;
import org.panda_lang.utilities.commons.FileUtils;
import org.panda_lang.utilities.commons.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MetadataService {

    private final Map<String, String> metadataCache = new HashMap<>();

    public String generateMetadata(Repository repository, String[] requested) throws IOException {
        File metadataFile = RepositoryUtils.toRequestedFile(repository, requested);
        String cachedContent = metadataCache.get(metadataFile.getPath());

        if (cachedContent != null) {
            NanoMaven.getLogger().debug("Served cached content");
            return cachedContent;
        }

        File artifactDirectory = metadataFile.getParentFile();

        if (artifactDirectory.isFile()) {
            return "Bad request";
        }

        File[] versions = MetadataUtils.toSortedVersions(artifactDirectory);

        if (versions.length > 0) {
            return generateArtifactMetadata(metadataFile, MetadataUtils.toGroup(requested, 2), artifactDirectory, versions);
        }

        return generateSnapshotMetadata(metadataFile, MetadataUtils.toGroup(requested, 3), artifactDirectory);
    }

    private String generateArtifactMetadata(File metadataFile, String groupId, File artifactDirectory, File[] list) throws IOException {
        File latest = MetadataUtils.getLatest(list);

        if (latest == null) {
            return "Empty artifact metadata";
        }

        Versions versions = Versions.of(list);
        Versioning versioning = new Versioning(latest.getName(), latest.getName(), versions, null, null, MetadataUtils.toUpdateTime(latest));
        Metadata metadata = new Metadata(groupId, artifactDirectory.getName(), null, versioning);

        return toMetadataFile(metadataFile, metadata);
    }

    private String generateSnapshotMetadata(File metadataFile, String groupId, File versionDirectory) throws IOException {
        File artifactDirectory = versionDirectory.getParentFile();
        File[] builds = MetadataUtils.toSortedBuilds(versionDirectory);
        File latestBuild = MetadataUtils.getLatest(builds);

        if (latestBuild == null) {
            return "Empty build metadata";
        }

        String name = artifactDirectory.getName();
        String version = StringUtils.replace(versionDirectory.getName(), "-SNAPSHOT", StringUtils.EMPTY);

        String[] identifiers = MetadataUtils.toSortedIdentifiers(name, version, builds);
        String latestIdentifier = Objects.requireNonNull(MetadataUtils.getLatest(identifiers));

        Snapshot snapshot = new Snapshot(latestIdentifier, builds.length);
        Collection<SnapshotVersion> snapshotVersions = new ArrayList<>(builds.length);

        for (String identifier : identifiers) {
            SnapshotVersion snapshotVersion = new SnapshotVersion("jar", name + "-" + version + "-" + identifier, identifier);
            snapshotVersions.add(snapshotVersion);
        }

        String latestRelease = name + "-" + version + "-" + latestIdentifier;
        Versioning versioning = new Versioning(latestRelease, latestRelease, null, snapshot, snapshotVersions, MetadataUtils.toUpdateTime(latestBuild));
        Metadata metadata = new Metadata(groupId, name, version, versioning);

        return toMetadataFile(metadataFile, metadata);
    }

    private String toMetadataFile(File metadataFile, Metadata metadata) throws IOException {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Metadata.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(metadata, metadataFile);
        } catch (JAXBException e) {
            e.printStackTrace();
            return "Internal error";
        }

        if (!FilesUtils.writeFileChecksums(metadataFile.toPath())) {
            NanoMaven.getLogger().error("Cannot write metadata checksums");
        }

        String content = FileUtils.getContentOfFile(metadataFile);
        metadataCache.put(metadataFile.getPath(), content);

        return content;
    }

    public void clearMetadata(File metadataFile) {
        metadataCache.remove(metadataFile.getPath());
    }

}
