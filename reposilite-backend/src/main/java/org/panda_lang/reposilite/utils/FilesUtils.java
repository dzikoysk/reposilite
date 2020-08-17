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
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.utilities.commons.IOUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.PandaStream;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FilesUtils {

    private static final File[] EMPTY = new File[0];

    private final static long KB_FACTOR = 1024;
    private final static long MB_FACTOR = 1024 * KB_FACTOR;
    private final static long GB_FACTOR = 1024 * MB_FACTOR;

    private FilesUtils() {}

    @SuppressWarnings({ "UnstableApiUsage", "deprecation" })
    public static void writeFileChecksums(Path path) throws IOException {
        Files.touch(new File(path + ".md5"));
        Files.touch(new File(path + ".sha1"));

        Path md5FileFile = Paths.get(path + ".md5");
        Path sha1FileFile = Paths.get(path + ".sha1");

        FileUtils.writeStringToFile(md5FileFile.toFile(), Files.hash(md5FileFile.toFile(), Hashing.md5()).toString(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(sha1FileFile.toFile(), Files.hash(sha1FileFile.toFile(), Hashing.sha1()).toString(), StandardCharsets.UTF_8);
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

    public static void copyResource(String resourcePath, File destination) throws IOException {
        URL inputUrl = Reposilite.class.getResource(resourcePath);
        FileUtils.copyURLToFile(inputUrl, destination);
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

    public static File[] listFiles(File directory) {
        File[] files = directory.listFiles();
        return files == null ? EMPTY : files;
    }

    public static List<String> toNames(File[] files) {
        return PandaStream.of(files)
                .map(File::getName)
                .toList();
    }

    public static String getExtension(String name) {
        int occurrence = name.lastIndexOf(".");
        return occurrence == -1 ? StringUtils.EMPTY : name.substring(occurrence + 1);
    }

    public static String getResource(String name) {
        return IOUtils.convertStreamToString(Reposilite.class.getResourceAsStream(name)).orElseThrow(ioException -> {
            throw new RuntimeException("Cannot load resource " + name, ioException);
        });
    }

}
