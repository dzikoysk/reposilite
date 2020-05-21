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
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FilesUtils {

    private static final File[] EMPTY = new File[0];

    private FilesUtils() {}

    @SuppressWarnings({ "UnstableApiUsage", "deprecation" })
    public static boolean writeFileChecksums(Path path) {
        try {
            Files.touch(new File(path + ".md5"));
            Files.touch(new File(path + ".sha1"));

            Path md5FileFile = Paths.get(path + ".md5");
            Path sha1FileFile = Paths.get(path + ".sha1");

            FileUtils.writeStringToFile(md5FileFile.toFile(), Files.hash(md5FileFile.toFile(), Hashing.md5()).toString(), StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(sha1FileFile.toFile(), Files.hash(sha1FileFile.toFile(), Hashing.sha1()).toString(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean copyResource(String resourcePath, String destinationPath) {
        URL inputUrl = Reposilite.class.getResource(resourcePath);
        File destination = new File(destinationPath);

        try {
            FileUtils.copyURLToFile(inputUrl, destination);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static File[] listFiles(File directory) {
        File[] files = directory.listFiles();
        return files == null ? EMPTY : files;
    }

    public static String getExtension(File file) {
        return getExtension(file.getName());
    }

    public static String getExtension(String name) {
        int occurrence = name.lastIndexOf(".");
        return occurrence == -1 ? StringUtils.EMPTY : name.substring(occurrence + 1);
    }

    public static boolean exists(String file) {
        return new File(file).exists();
    }

}
