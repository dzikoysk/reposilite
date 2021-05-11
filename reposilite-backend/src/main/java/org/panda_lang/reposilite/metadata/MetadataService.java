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
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.repository.FileDetailsDto;
import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.collection.Pair;
import org.panda_lang.utilities.commons.function.Lazy;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public final class MetadataService {

    private static final Lazy<XmlMapper> XML_MAPPER = new Lazy<>(() -> XmlMapper.xmlBuilder()
            .serializationInclusion(Include.NON_NULL)
            .defaultUseWrapper(false)
            .build());

    private final Map<Path, Pair<FileDetailsDto, String>> metadataCache = new HashMap<>();
    private final FailureService failureService;

    public MetadataService(FailureService failureService) {
        this.failureService = failureService;
    }

    public Result<Pair<FileDetailsDto, String>, ErrorDto> getMetadata(Repository repository, Path requested) throws IOException {
        if (!requested.getFileName().toString().equals("maven-metadata.xml")) {
            return Result.error(new ErrorDto(HttpStatus.SC_BAD_REQUEST, "Bad request"));
        }

        Pair<FileDetailsDto, String> cachedContent = metadataCache.get(requested);

        if (cachedContent != null) {
            return Result.ok(cachedContent);
        }

        Path artifactDirectory = requested.getParent();

        if (repository.exists(artifactDirectory)) {
            return Result.error(new ErrorDto(HttpStatus.SC_BAD_REQUEST, "Bad request"));
        }

        Result<Path[], ErrorDto> versions = MetadataUtils.toSortedVersions(repository, artifactDirectory);

        if (versions.isErr()) return versions.map(p -> null);

        if (versions.get().length > 0) {
            return generateArtifactMetadata(repository, requested, MetadataUtils.toGroup(requested), artifactDirectory, versions.get());
        }

        return generateBuildMetadata(repository, requested, MetadataUtils.toGroup(requested), artifactDirectory);
    }

    private Result<Pair<FileDetailsDto, String>, ErrorDto> generateArtifactMetadata(Repository repository, Path metadataFile, String groupId, Path artifactDirectory, Path[] versions) throws IOException {
        Path latest = Objects.requireNonNull(ArrayUtils.getFirst(versions));

        Versioning versioning = new Versioning(latest.getFileName().toString(), latest.getFileName().toString(), FilesUtils.toNames(versions), null, null, MetadataUtils.toUpdateTime(repository, latest));
        Metadata metadata = new Metadata(groupId, artifactDirectory.getFileName().toString(), null, versioning);

        return toMetadataFile(repository, metadataFile, metadata);
    }

    private Result<Pair<FileDetailsDto, String>, ErrorDto> generateBuildMetadata(Repository repository, Path metadataFile, String groupId, Path versionDirectory) throws IOException {
        Path artifactDirectory = versionDirectory.getParent();
        Result<Path[], ErrorDto> builds = MetadataUtils.toSortedBuilds(repository, versionDirectory);

        if (builds.isErr()) return builds.map(p -> null);

        Path latestBuild = ArrayUtils.getFirst(builds.get());

        if (latestBuild == null) {
            return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "Latest build not found"));
        }

        String name = artifactDirectory.getFileName().toString();
        String version = StringUtils.replace(versionDirectory.getFileName().toString(), "-SNAPSHOT", StringUtils.EMPTY);

        String[] identifiers = MetadataUtils.toSortedIdentifiers(repository, name, version, builds.get());
        String latestIdentifier = Objects.requireNonNull(ArrayUtils.getFirst(identifiers));
        int buildSeparatorIndex = latestIdentifier.lastIndexOf("-");
        Versioning versioning;

        // snapshot requests
        if (buildSeparatorIndex != -1) {

            // format: timestamp-buildNumber
            String latestTimestamp = latestIdentifier.substring(0, buildSeparatorIndex);
            String latestBuildNumber = latestIdentifier.substring(buildSeparatorIndex + 1);

            Snapshot snapshot = new Snapshot(latestTimestamp, latestBuildNumber);
            Collection<SnapshotVersion> snapshotVersions = new ArrayList<>(builds.get().length);

            for (String identifier : identifiers) {
                Path[] buildFiles = MetadataUtils.toBuildFiles(repository, versionDirectory, identifier);

                for (Path buildFile : buildFiles) {
                    String fileName = buildFile.getFileName().toString();
                    String value = version + "-" + identifier;
                    String updated = MetadataUtils.toUpdateTime(repository, buildFile);
                    String extension = fileName
                            .replace(name + "-", StringUtils.EMPTY)
                            .replace(value + ".", StringUtils.EMPTY);

                    SnapshotVersion snapshotVersion = new SnapshotVersion(extension, value, updated);
                    snapshotVersions.add(snapshotVersion);
                }
            }

            versioning = new Versioning(null, null, null, snapshot, snapshotVersions, MetadataUtils.toUpdateTime(repository, latestBuild));
        }
        else {
            String fullVersion = version + "-SNAPSHOT";
            versioning = new Versioning(fullVersion, fullVersion, Collections.singletonList(fullVersion), null, null, MetadataUtils.toUpdateTime(repository, latestBuild));
        }

        return toMetadataFile(repository, metadataFile, new Metadata(groupId, name, versionDirectory.getFileName().toString(), versioning));
    }

    private Result<Pair<FileDetailsDto, String>, ErrorDto> toMetadataFile(Repository repository, Path metadataFile, Metadata metadata) {
        try {
            String serializedMetadata = XML_MAPPER.get().writeValueAsString(metadata);
            byte[] bytes = serializedMetadata.getBytes(StandardCharsets.UTF_8);
            Result<FileDetailsDto, ErrorDto> result = repository.putFile(metadataFile, bytes);

            if (result.isOk()) {
                FilesUtils.writeFileChecksums(repository, metadataFile, bytes);
                metadataCache.put(metadataFile, new Pair<>(result.get(), serializedMetadata));
            }

            return result.map(fileDetailsDto -> new Pair<>(fileDetailsDto, serializedMetadata));
        } catch (IOException e) {
            failureService.throwException(metadataFile.toAbsolutePath().toString(), e);
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Cannot generate metadata"));
        }
    }

    public void clearMetadata(Path metadataFile) {
        metadataCache.remove(metadataFile);
    }

    public int purgeCache() {
        int count = getCacheSize();
        metadataCache.clear();
        return count;
    }

    public int getCacheSize() {
        return metadataCache.size();
    }

}
