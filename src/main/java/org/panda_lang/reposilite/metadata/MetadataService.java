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

package org.panda_lang.reposilite.metadata;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.jetbrains.annotations.Nullable;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.reposilite.repository.RepositoryUtils;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.FileUtils;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MetadataService {

    private final Map<String, String> metadataCache = new HashMap<>();

    private final XmlMapper xmlMapper = XmlMapper.xmlBuilder()
            .serializationInclusion(Include.NON_NULL)
            .defaultUseWrapper(false)
            .build();

    public @Nullable String generateMetadata(Repository repository, String[] requested) throws IOException {
        File metadataFile = RepositoryUtils.toRequestedFile(repository, requested);
        String cachedContent = metadataCache.get(metadataFile.getPath());

        if (cachedContent != null) {
            Reposilite.getLogger().debug("Served cached content");
            return cachedContent;
        }

        File artifactDirectory = metadataFile.getParentFile();

        if (artifactDirectory.isFile()) {
            Reposilite.getLogger().warn("Bad request");
            return null;
        }

        File[] versions = MetadataUtils.toSortedVersions(artifactDirectory);

        if (versions.length > 0) {
            return generateArtifactMetadata(metadataFile, MetadataUtils.toGroup(requested, 2), artifactDirectory, versions);
        }

        return generateSnapshotMetadata(metadataFile, MetadataUtils.toGroup(requested, 3), artifactDirectory);
    }

    private @Nullable String generateArtifactMetadata(File metadataFile, String groupId, File artifactDirectory, File[] versions) throws IOException {
        File latest = MetadataUtils.getLatest(versions);

        if (latest == null) {
            return null;
        }

        Versioning versioning = new Versioning(latest.getName(), latest.getName(), MetadataUtils.toNames(versions), null, null, MetadataUtils.toUpdateTime(latest));
        Metadata metadata = new Metadata(groupId, artifactDirectory.getName(), null, versioning);

        return toMetadataFile(metadataFile, metadata);
    }

    private @Nullable String generateSnapshotMetadata(File metadataFile, String groupId, File versionDirectory) throws IOException {
        File artifactDirectory = versionDirectory.getParentFile();
        File[] builds = MetadataUtils.toSortedBuilds(versionDirectory);
        File latestBuild = MetadataUtils.getLatest(builds);

        if (latestBuild == null) {
            return null;
        }

        String name = artifactDirectory.getName();
        String version = StringUtils.replace(versionDirectory.getName(), "-SNAPSHOT", StringUtils.EMPTY);

        String[] identifiers = MetadataUtils.toSortedIdentifiers(name, version, builds);
        String latestIdentifier = Objects.requireNonNull(MetadataUtils.getLatest(identifiers));
        int buildSeparatorIndex = latestIdentifier.lastIndexOf("-");

        // not a snapshot request, missing build number
        if (buildSeparatorIndex == -1) {
            return null;
        }

        String latestTimestamp = latestIdentifier.substring(0, buildSeparatorIndex);
        String latestBuildNumber = latestIdentifier.substring(buildSeparatorIndex + 1);

        Snapshot snapshot = new Snapshot(latestTimestamp, latestBuildNumber);
        Collection<SnapshotVersion> snapshotVersions = new ArrayList<>(builds.length);

        for (String identifier : identifiers) {
            File[] buildFiles = MetadataUtils.toBuildFiles(versionDirectory, identifier);

            for (File buildFile : buildFiles) {
                String fileName = buildFile.getName();
                String value = version + "-" + identifier;
                String classifier = FilesUtils.getExtension(buildFile);
                String updated = MetadataUtils.toUpdateTime(buildFile);

                if (StringUtils.countOccurrences(fileName, "-") >= 4) {
                    classifier = fileName
                            .replace(name + "-", StringUtils.EMPTY)
                            .replace(value + "-", StringUtils.EMPTY)
                            .replace("." + classifier, StringUtils.EMPTY);
                }

                SnapshotVersion snapshotVersion = new SnapshotVersion(classifier, value, updated);
                snapshotVersions.add(snapshotVersion);
            }
        }

        Versioning versioning = new Versioning(null, null, null, snapshot, snapshotVersions, MetadataUtils.toUpdateTime(latestBuild));
        Metadata metadata = new Metadata(groupId, name, versionDirectory.getName(), versioning);

        return toMetadataFile(metadataFile, metadata);
    }

    private String toMetadataFile(File metadataFile, Metadata metadata) throws IOException {
        String serializedMetadata = xmlMapper.writeValueAsString(metadata);
        FileUtils.overrideFile(metadataFile, serializedMetadata);
        metadataCache.put(metadataFile.getPath(), serializedMetadata);

        if (!FilesUtils.writeFileChecksums(metadataFile.toPath())) {
            Reposilite.getLogger().error("Cannot write metadata checksums");
        }

        return serializedMetadata;
    }

    public void clearMetadata(File metadataFile) {
        metadataCache.remove(metadataFile.getPath());
    }

    public void purgeCache() {
        metadataCache.clear();
    }

    public int getCacheSize() {
        return metadataCache.size();
    }

}
