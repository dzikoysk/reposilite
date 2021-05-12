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

import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.storage.StorageProvider;
import org.panda_lang.utilities.commons.function.Result;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;

public final class Repository implements Comparator<Path> {

    private static final Path REPOSITORIES = Paths.get("repositories");

    private final String name;
    private final RepositoryVisibility visibility;
    private final StorageProvider storageProvider;
    private final boolean deployEnabled;

    Repository(String name, RepositoryVisibility visibility, StorageProvider storageProvider, boolean deployEnabled) {
        this.name = name;
        this.visibility = visibility;
        this.storageProvider = storageProvider;
        this.deployEnabled = deployEnabled;
    }

    public boolean isDeployEnabled() {
        return deployEnabled;
    }

    public boolean isPublic() {
        return !isPrivate();
    }

    public boolean isPrivate() {
        return this.visibility == RepositoryVisibility.PRIVATE;
    }

    public String getName() {
        return this.name;
    }

    public Result<FileDetailsDto, ErrorDto> putFile(Path file, byte[] bytes) {
        return this.storageProvider.putFile(this.relativize(file), bytes);
    }

    public Result<FileDetailsDto, ErrorDto> putFile(Path file, InputStream inputStream) {
        return this.storageProvider.putFile(this.relativize(file), inputStream);
    }

    public Result<byte[], ErrorDto> getFile(Path file) {
        return this.storageProvider.getFile(this.relativize(file));
    }

    public Result<FileDetailsDto, ErrorDto> getFileDetails(Path file) {
        return this.storageProvider.getFileDetails(this.relativize(file));
    }

    public Result<Void, ErrorDto> removeFile(Path file) {
        return this.storageProvider.removeFile(this.relativize(file));
    }

    public Result<List<Path>, ErrorDto> getFiles(Path directory) {
        return this.storageProvider.getFiles(this.relativize(directory));
    }

    public Result<FileTime, ErrorDto> getLastModifiedTime(Path file) {
        return this.storageProvider.getLastModifiedTime(this.relativize(file));
    }

    public Result<Long, ErrorDto> getFileSize(Path file) {
        return this.storageProvider.getFileSize(this.relativize(file));
    }

    public boolean exists(Path file) {
        return this.storageProvider.exists(this.relativize(file));
    }

    public boolean isDirectory(Path file) {
        return this.storageProvider.isDirectory(this.relativize(file));
    }

    public boolean isFull() {
        return this.storageProvider.isFull();
    }

    public long getUsage() {
        return this.storageProvider.getUsage();
    }

    public boolean canHold(long contentLength) {
        return this.storageProvider.canHold(contentLength);
    }

    public void shutdown() {
        this.storageProvider.shutdown();
    }

    public Path relativize(Path path) {
        if (path == null) return null;

        if (!path.startsWith(REPOSITORIES)) {
            if (!path.startsWith(this.name)) {
                path = Paths.get(this.name).resolve(path);
            }

            path = REPOSITORIES.resolve(path);
        } else if (path.startsWith(this.name)) {
            path = REPOSITORIES.relativize(path);
        }

        return path;
    }

    @Override
    public int compare(Path o1, Path o2) {
        return this.relativize(o1).compareTo(this.relativize(o2));
    }

}
