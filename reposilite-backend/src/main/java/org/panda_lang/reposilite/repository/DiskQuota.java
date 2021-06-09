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
import org.panda_lang.reposilite.utils.FilesUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

public final class DiskQuota {

    private final AtomicLong quota;
    private final AtomicLong usage;

    DiskQuota(long quota, long usage) {
        this.quota = new AtomicLong(quota);
        this.usage = new AtomicLong(usage);
    }

    void allocate(long size) {
        usage.addAndGet(size);
    }

    public boolean hasUsableSpace() {
        return usage.get() < quota.get();
    }

    public long getUsage() {
        return usage.get();
    }

    static DiskQuota ofPercentage(File workingDirectory, long usage, int percentage) {
        return new DiskQuota(Math.round(workingDirectory.getTotalSpace() * (percentage / 100D)), usage);
    }

    static DiskQuota ofSize(long usage, String size) {
        return new DiskQuota(FilesUtils.displaySizeToBytesCount(size), usage);
    }

    static DiskQuota of(File workingDirectory, String value) {
        long usage = FileUtils.sizeOfDirectory(workingDirectory);

        if (value.endsWith("%")) {
            return ofPercentage(workingDirectory,  usage, Integer.parseInt(value.substring(0, value.length() - 1)));
        }

        return ofSize(usage, value);
    }

}
