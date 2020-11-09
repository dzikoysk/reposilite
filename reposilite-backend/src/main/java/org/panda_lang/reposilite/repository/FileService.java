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

import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.metadata.MetadataUtils;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.function.Option;
import org.panda_lang.utilities.commons.function.PandaStream;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public final class FileService {

    private static final int INITIAL_CACHE_SIZE = 1024;

    private final int initialCache;
    private final ExecutorService ioService;
    private final Map<String, FileListDto> cache;

    public FileService(ExecutorService ioService, int initialCache) {
        this.ioService = ioService;
        this.initialCache = initialCache;
        this.cache = isCacheEnabled() ? new HashMap<>(INITIAL_CACHE_SIZE) : Collections.emptyMap();
    }

    public void loadInitialRepositoryCache(File repositoryRoot) {
        submitCachedTask(() -> {
            Reposilite.getLogger().info("Preparing repository cache...");
            createFileListDto(repositoryRoot, 0);
            Reposilite.getLogger().info("Gathered cache: " + cache.size());
        });
    }

    public void forceCacheUpdate(File parent) {
        submitCachedTask(() -> createFileListDto(parent, initialCache));
    }

    private void submitCachedTask(Runnable task) {
        if (isCacheEnabled()) {
            ioService.submit(task);
        }
    }

    public FileListDto toFileListDto(File parent) {
        return Option.when(isCacheEnabled(), cache.get(parent.getAbsolutePath()))
                .orElseGet(() -> createFileListDto(parent, Integer.MAX_VALUE));
    }

    private FileListDto createFileListDto(File[] files) {
        return new FileListDto(PandaStream.of(files)
                .map(FileDetailsDto::of)
                .transform(stream -> MetadataUtils.toSorted(stream, FileDetailsDto::getName, FileDetailsDto::isDirectory))
                .toList());
    }

    private FileListDto createFileListDto(File parent, int level) {
        if (parent.isFile()) {
            return new FileListDto(Collections.singletonList(FileDetailsDto.of(parent)));
        }

        File[] files = FilesUtils.listFiles(parent);
        FileListDto listDto = createFileListDto(files);

        if (level < initialCache) {
            cache.put(parent.getAbsolutePath(), listDto);

            for (File subParent : files) {
                createFileListDto(subParent, level + 1);
            }
        }

        return listDto;
    }

    public boolean isCacheEnabled() {
        return this.initialCache > 0;
    }

}
