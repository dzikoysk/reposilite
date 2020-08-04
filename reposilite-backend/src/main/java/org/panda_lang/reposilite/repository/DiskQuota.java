package org.panda_lang.reposilite.repository;

import org.apache.commons.io.FileUtils;
import org.panda_lang.reposilite.utils.FilesUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

final class DiskQuota {

    private final AtomicLong quota;
    private final AtomicLong usage;

    public DiskQuota(long quota, long usage) {
        this.quota = new AtomicLong(quota);
        this.usage = new AtomicLong(usage);
    }

    public synchronized boolean allocate(long size) {
        if (usage.get() + size >= quota.get()) {
            return false;
        }

        usage.addAndGet(size);
        return true;
    }

    public static DiskQuota ofPercentage(File workingDirectory, long usage, int percentage) {
        return new DiskQuota(Math.round(workingDirectory.getUsableSpace() * (percentage / 100D)), usage);
    }

    public static DiskQuota ofSize(long usage, String size) {
        return new DiskQuota(FilesUtils.displaySizeToBytesCount(size), usage);
    }

    public static DiskQuota of(File workingDirectory, String value) {
        long usage = FileUtils.sizeOfDirectory(workingDirectory);

        if (value.endsWith("%")) {
            return ofPercentage(workingDirectory,  usage, Integer.parseInt(value.substring(0, value.length() - 1)));
        }
        else {
            return ofSize(usage, value);
        }
    }

}
