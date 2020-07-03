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

package org.panda_lang.reposilite.repository;

import org.jetbrains.annotations.NotNull;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.text.SimpleDateFormat;

final class FileDto implements Serializable, Comparable<FileDto> {

    public static final String FILE = "file";
    public static final String DIRECTORY = "directory";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    private final String type;
    private final String name;
    private final String date;
    private final long contentLength;

    private FileDto(String type, String name, String date, long contentLength) {
        this.type = type;
        this.name = name;
        this.date = date;
        this.contentLength = contentLength;
    }

    @Override
    public int compareTo(@NotNull FileDto to) {
        int result = type.compareTo(to.getType());

        if (result == 0) {
            result = name.compareTo(to.getName());
        }

        return result;
    }

    boolean isDirectory() {
        return DIRECTORY.equals(type);
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public static FileDto of(File file) {
        String date = StringUtils.EMPTY;

        try {
            date = DATE_FORMAT.format(Files.getLastModifiedTime(file.toPath()).toMillis());
        }
        catch (IOException ignored) { /* file does not exist */ }

        return new FileDto(
                file.isDirectory() ? DIRECTORY : FILE,
                file.getName(),
                date,
                file.isDirectory() ? -1 : file.length()
        );
    }

}
