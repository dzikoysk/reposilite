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
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;

final class FileDetailsDto implements Serializable, Comparable<FileDetailsDto> {

    public static final String FILE = "file";
    public static final String DIRECTORY = "directory";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    private final String type;
    private final String name;
    private final String date;
    private final String contentType;
    private final long contentLength;

    public FileDetailsDto(String type, String name, String date, String contentType, long contentLength) {
        this.type = type;
        this.name = name;
        this.date = date;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    @Override
    public int compareTo(@NotNull FileDetailsDto to) {
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

    public String getContentType() {
        return contentType;
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

    public static FileDetailsDto of(Path file) {
        String date = StringUtils.EMPTY;
        String contentType = FilesUtils.getMimeType(file.getAbsolutePath(), "application/octet-stream");

        long size = -1;

        try {
            date = DATE_FORMAT.format(Files.getLastModifiedTime(file).toMillis());
            size = Files.size(file);
        }
        catch (IOException ignored) { /* file does not exist */ }

        return new FileDetailsDto(
                Files.isDirectory(file) ? DIRECTORY : FILE,
                file.getFileName().toString(),
                date,
                contentType,
                Files.isDirectory(file) ? -1 : size);
    }

}
