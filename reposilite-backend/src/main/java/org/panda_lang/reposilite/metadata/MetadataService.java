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
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.reposilite.utils.Result;
import org.panda_lang.utilities.commons.FileUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Lazy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MetadataService {

    private static final Lazy<XmlMapper> XML_MAPPER = new Lazy<>(() -> XmlMapper.xmlBuilder()
            .serializationInclusion(Include.NON_NULL)
            .defaultUseWrapper(false)
            .build());

    private final Reposilite reposilite;
    private final Map<String, String> metadataCache = new HashMap<>();

    public MetadataService(Reposilite reposilite) {
        this.reposilite = reposilite;
    }

    public Result<String, String> generateMetadata(Repository repository, String[] requested) {
        File metadataFile = repository.getFile(requested);
        String cachedContent = metadataCache.get(metadataFile.getPath());

        if (cachedContent != null) {
            return Result.ok(cachedContent);
        }

        File artifactDirectory = metadataFile.getParentFile();

        if (artifactDirectory.isFile()) {
            return Result.error("Bad request");
        }

        File[] versions = MetadataUtils.toSortedVersions(artifactDirectory);

        if (versions.length > 0) {
            return generateArtifactMetadata(metadataFile, MetadataUtils.toGroup(requested, 2), artifactDirectory, versions);
        }

        return generateBuildMetadata(metadataFile, MetadataUtils.toGroup(requested, 3), artifactDirectory);
    }

    private Result<String, String> generateArtifactMetadata(File metadataFile, String groupId, File artifactDirectory, File[] versions) {
        File latest = ArrayUtils.getLatest(versions);

        if (latest == null) {
            return Result.error("Artifact does not have any releases");
        }

        Versioning versioning = new Versioning(latest.getName(), latest.getName(), FilesUtils.toNames(versions), null, null, MetadataUtils.toUpdateTime(latest));
        Metadata metadata = new Metadata(groupId, artifactDirectory.getName(), null, versioning);

        return toMetadataFile(metadataFile, metadata);
    }

    private Result<String, String> generateBuildMetadata(File metadataFile, String groupId, File versionDirectory) {
        File artifactDirectory = versionDirectory.getParentFile();
        File[] builds = MetadataUtils.toSortedBuilds(versionDirectory);
        File latestBuild = ArrayUtils.getLatest(builds);

        if (latestBuild == null) {
            return Result.error("Version does not have any builds");
        }

        String name = artifactDirectory.getName();
        String version = StringUtils.replace(versionDirectory.getName(), "-SNAPSHOT", StringUtils.EMPTY);

        String[] identifiers = MetadataUtils.toSortedIdentifiers(name, version, builds);
        String latestIdentifier = Objects.requireNonNull(ArrayUtils.getLatest(identifiers));
        int buildSeparatorIndex = latestIdentifier.lastIndexOf("-");
        Versioning versioning;

        // snapshot requests
        if (buildSeparatorIndex != -1) {
            // format: timestamp-buildNumber
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

            versioning = new Versioning(null, null, null, snapshot, snapshotVersions, MetadataUtils.toUpdateTime(latestBuild));
        }
        else {
            String fullVersion = version + "-SNAPSHOT";
            versioning = new Versioning(fullVersion, fullVersion, Collections.singletonList(fullVersion), null, null, MetadataUtils.toUpdateTime(latestBuild));
        }

        return toMetadataFile(metadataFile, new Metadata(groupId, name, versionDirectory.getName(), versioning));
    }

    private Result<String, String> toMetadataFile(File metadataFile, Metadata metadata) {
        try {
            String serializedMetadata = XML_MAPPER.get().writeValueAsString(metadata);
            FileUtils.overrideFile(metadataFile, serializedMetadata);
            metadataCache.put(metadataFile.getPath(), serializedMetadata);

            if (!FilesUtils.writeFileChecksums(metadataFile.toPath())) {
                Reposilite.getLogger().error("Cannot write metadata checksums");
            }

            return Result.ok(serializedMetadata);
        } catch (IOException e) {
            reposilite.throwException(metadataFile.getAbsolutePath(), e);
            return Result.error("Cannot generate metadata");
        }
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
