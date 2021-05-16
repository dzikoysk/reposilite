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

package org.panda_lang.reposilite.utils;

import com.google.common.hash.Hashing;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.PandaStream;
import org.panda_lang.utilities.commons.function.Result;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FilesUtils {
    private final static long KB_FACTOR = 1024;
    private final static long MB_FACTOR = 1024 * KB_FACTOR;
    private final static long GB_FACTOR = 1024 * MB_FACTOR;

    private static final String[] READABLE_CONTENT = {
            ".xml",
            ".pom",
            ".txt",
            ".json",
            ".cdn",
            ".yaml",
            ".yml"
    };

    private FilesUtils() {}

    @SuppressWarnings({ "UnstableApiUsage", "deprecation" })
    public static void writeFileChecksums(Repository repository, Path path, byte[] bytes) throws IOException {
        path = repository.relativize(path);
        Path md5 = path.resolveSibling(path.getFileName() + ".md5");
        Path sha1 = path.resolveSibling(path.getFileName() + ".sha1");
        Path sha256 = path.resolveSibling(path.getFileName() + ".sha256");
        Path sha512 = path.resolveSibling(path.getFileName() + ".sha512");

        repository.putFile(md5, Hashing.md5().hashBytes(bytes).asBytes());
        repository.putFile(sha1, Hashing.sha1().hashBytes(bytes).asBytes());
        repository.putFile(sha256, Hashing.sha256().hashBytes(bytes).asBytes());
        repository.putFile(sha512, Hashing.sha512().hashBytes(bytes).asBytes());
    }

    public static long displaySizeToBytesCount(String displaySize) {
        Pattern pattern = Pattern.compile("([0-9]+)(([KkMmGg])[Bb])");
        Matcher match = pattern.matcher(displaySize);

        if (!match.matches() || match.groupCount() != 3) {
            return Long.parseLong(displaySize);
        }

        long value = Long.parseLong(match.group(1));

        switch (match.group(2).toUpperCase()) {
            case "GB":
                return value * GB_FACTOR;
            case "MB":
                return value * MB_FACTOR;
            case "KB":
                return value * KB_FACTOR;
            default:
                throw new NumberFormatException("Wrong format");
        }
    }

    // Source
    // ~ https://stackoverflow.com/a/3758880/3426515
    public static String humanReadableByteCount(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);

        if (absB < 1024) {
            return bytes + " B";
        }

        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");

        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }

        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    public static boolean isReadable(String name) {
        for (String extension : READABLE_CONTENT) {
            if (name.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    public static String getMimeType(String path, String defaultType) {
        return MimeTypes.getMimeType(getExtension(path), defaultType);
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // idc
            }
        }
    }

    public static List<String> toNames(Path[] files) {
        return PandaStream.of(files)
                .map(path -> path.getFileName().toString())
                .toList();
    }

    public static String getExtension(String name) {
        int occurrence = name.lastIndexOf(".");
        return occurrence == -1 ? StringUtils.EMPTY : name.substring(occurrence + 1);
    }

    public static String getResource(String name) {
        return org.panda_lang.utilities.commons.IOUtils.convertStreamToString(Reposilite.class.getResourceAsStream(name))
                .orElseThrow(ioException -> {
                    throw new RuntimeException("Cannot load resource " + name, ioException);
                });
    }

}
