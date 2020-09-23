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

import org.apache.commons.io.FileUtils;
import org.panda_lang.utilities.commons.function.Option;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

final class RepositoryFile {

    private static final long RETRY_WRITE_TIME = 2000L;
    private static final int RETRY_LIMIT = 5;

    private final Set<RepositoryFile> LOCKS = new HashSet<>();

    private final File file;
    private final String absolute;
    private final DiskQuota diskQuota;
    private Option<FileLock> lock = Option.none();

    RepositoryFile(DiskQuota diskQuota, File file) {
        this.file = file;
        this.absolute = file.getAbsolutePath();
        this.diskQuota = diskQuota;
    }

    void store(InputStream source) throws Exception {
        store(source, 0);
    }

    private void store(InputStream source, int attempt) throws Exception {
        Path target = file.toPath();
        FileUtils.forceMkdirParent(file);

        synchronized (this) {
            if (LOCKS.contains(this)) {
                retry(source, attempt);
                return;
            }

            LOCKS.add(this);
        }

        try (FileChannel channel = FileChannel.open(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            this.lock = Option.of(channel.lock());

            channel.truncate(0L).transferFrom(Channels.newChannel(source), 0, Long.MAX_VALUE);
            //Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            diskQuota.allocate(file.length());
        } catch (OverlappingFileLockException exception) {
            release();
            retry(source, attempt);
        } finally {
            release();
        }
    }

    private synchronized void release() {
        this.lock = lock.flatMap(fileLock -> {
            try {
                fileLock.release();
            }
            catch (IOException ignored) {
                // muted, it's closed or invalid anyway
            }

            return Option.none();
        });

        LOCKS.remove(this);
    }

    private void retry(InputStream source, int currentAttempt) throws Exception {
        if (currentAttempt == RETRY_LIMIT) {
            throw new TimeoutException("Cannot access " + file);
        }

        Thread.sleep(RETRY_WRITE_TIME);
        store(source, currentAttempt + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RepositoryFile that = (RepositoryFile) o;
        return absolute.equals(that.absolute);
    }

    @Override
    public int hashCode() {
        return absolute.hashCode();
    }

}
