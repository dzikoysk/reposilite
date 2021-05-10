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

import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.PandaStream;
import org.panda_lang.utilities.commons.function.Result;
import org.panda_lang.utilities.commons.text.Joiner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MetadataUtils {

    public static final String ESCAPE_DOT = "`.`";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneOffset.UTC);

    private MetadataUtils() { }

    public static Result<Path[], ErrorDto> toSortedBuilds(Repository repository, Path directory) {
        Result<List<Path>, ErrorDto> result = repository.getFiles(directory);

        if (result.isOk()) {
            Collection<Path> paths = new TreeSet<>(repository);

            for (Path path : result.get()) {
                paths.add(directory.resolve(path.getName(0)));
            }

            return Result.ok(paths.toArray(new Path[0]));
        }

        return result.map(p -> null);
    }

    public static Result<Path[], ErrorDto> toSortedVersions(Repository repository, Path directory) {
        return repository.getFiles(directory).map(list -> list.stream()
                .filter(path -> path.getParent().endsWith(directory))
                .filter(repository::isDirectory)
                .sorted(repository)
                .toArray(Path[]::new)
        );
    }

    protected static String[] toSortedIdentifiers(Repository repository, String artifact, String version, Path[] builds) {
        return PandaStream.of(builds)
                .sorted(repository)
                .map(build -> toIdentifier(artifact, version, build))
                .filterNot(StringUtils::isEmpty)
                .distinct()
                .toArray(String[]::new);
    }

    protected static boolean isNotChecksum(Path path, String identifier) {
        String name = path.getFileName().toString();

        return (!name.endsWith(".md5"))
                && (!name.endsWith(".sha1"))
                && (name.contains(identifier + ".") || name.contains(identifier + "-"));
    }

    protected static Path[] toBuildFiles(Repository repository, Path directory, String identifier) {
        return repository.getFiles(directory).get().stream()
                .filter(path -> path.getParent().equals(directory))
                .filter(path -> isNotChecksum(path, identifier))
                .filter(repository::exists)
                .sorted(repository)
                .toArray(Path[]::new);
    }

    public static String toIdentifier(String artifact, String version, Path build) {
        String identifier = build.getFileName().toString();
        identifier = StringUtils.replace(identifier, "." + FilesUtils.getExtension(identifier), StringUtils.EMPTY);
        identifier = StringUtils.replace(identifier, artifact + "-", StringUtils.EMPTY);
        identifier = StringUtils.replace(identifier, version + "-", StringUtils.EMPTY);
        return declassifyIdentifier(identifier);
    }

    private static String declassifyIdentifier(String identifier) {
        int occurrences = StringUtils.countOccurrences(identifier, "-");

        // no action required
        if (occurrences == 0) {
            return identifier;
        }

        int occurrence = identifier.indexOf("-");

        // process identifiers without classifier or build number
        if (occurrences == 1) {
            return isBuildNumber(identifier.substring(occurrence + 1)) ? identifier : identifier.substring(0, occurrence);
        }

        // remove classifier
        return isBuildNumber(identifier.substring(occurrence + 1, identifier.indexOf("-", occurrence + 1)))
                ? identifier.substring(0, identifier.indexOf("-", occurrence + 1))
                : identifier.substring(0, occurrence);
    }

    protected static String toUpdateTime(Repository repository, Path file) throws IOException {
        Result<FileTime, ErrorDto> result = repository.getLastModifiedTime(file);

        if (result.isOk()) {
            return TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(result.get().toMillis()));
        } else {
            throw new IOException(result.getError().getMessage());
        }
    }

    public static String toGroup(String[] elements) {
        return Joiner.on(".")
                .join(Arrays.copyOfRange(elements, 0, elements.length), value -> value.contains(".") ? value.replace(".", ESCAPE_DOT) : value)
                .toString();
    }

    protected static String toGroup(String[] elements, int toShrink) {
        return toGroup(shrinkGroup(elements, toShrink)).replace(ESCAPE_DOT, ".");
    }

    private static String[] shrinkGroup(String[] elements, int toShrink) {
        return Arrays.copyOfRange(elements, 0, elements.length - toShrink);
    }

    public static String toGroup(Path metadataFilePath) {
        StringBuilder builder = new StringBuilder();

        for (Iterator<Path> iterator = metadataFilePath.getParent().getParent().iterator(); iterator.hasNext(); ) {
            Path path = iterator.next();
            builder.append(path.getFileName().toString());

            if (iterator.hasNext()) {
                builder.append('.');
            }
        }

        return builder.toString();
    }

    private static boolean isBuildNumber(String content) {
        for (char character : content.toCharArray()) {
            if (!Character.isDigit(character)) {
                return false;
            }
        }

        return true;
    }

}
