package org.panda_lang.reposilite.storage;

import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.function.mutable.Mutable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class FileSystemStorageProvider extends StorageProvider {
    protected final Path rootDirectory;

    public static FileSystemStorageProvider of(Logger logger, Path rootDirectory, String quota) {
        if (quota.endsWith("%")) {
            return of(logger, rootDirectory, Integer.parseInt(quota.substring(0, quota.length() - 1)));
        } else {
            return of(logger, rootDirectory, FilesUtils.displaySizeToBytesCount(quota));
        }
    }

    /**
     * @param rootDirectory root directory of storage space
     * @param maxSize the largest amount of storage available for use, in bytes
     */
    public static FileSystemStorageProvider of(Logger logger, Path rootDirectory, long maxSize) {
        return new FixedQuota(logger, rootDirectory, maxSize);
    }

    /**
     * @param rootDirectory root directory of storage space
     * @param maxPercentage the maximum percentage of the disk available for use
     */
    public static FileSystemStorageProvider of(Logger logger, Path rootDirectory, double maxPercentage) {
        return new PercentageQuota(logger, rootDirectory, maxPercentage);
    }

    /**
     * @param rootDirectory root directory of storage space
     */
    private FileSystemStorageProvider(Logger logger, Path rootDirectory) {
        super(logger);
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void putFile(Path file, byte[] bytes) {
        try {
            Path destination = this.rootDirectory.resolve(file);

            if (destination.getParent() != null && !Files.exists(destination.getParent())) {
                Files.createDirectories(destination.getParent());
            }

            Files.write(file, bytes);
        } catch (IOException e) {
            this.logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void putFile(Path file, byte[] bytes, String contentType) {
        this.putFile(file, bytes);
    }

    @Override
    public byte[] getFile(Path file) {
        if (!Files.exists(file) || Files.isDirectory(file)) {
            this.logger.error("File " + file + " not found");
            return new byte[0];
        }

        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            this.logger.error(e.getLocalizedMessage());
            return new byte[0];
        }
    }

    @Override
    public void removeFile(Path file) {
        try {
            Files.delete(file);
        } catch (IOException e) {
            this.logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public List<Path> getFiles(Path directory) {
        try {
            return Files.walk(directory).collect(Collectors.toList());
        } catch (IOException e) {
            this.logger.error(e.getLocalizedMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public FileTime getLastModifiedTime(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            this.logger.error(e.getLocalizedMessage());
            return null;
        }
    }

    @Override
    public long getFileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            this.logger.error(e.getLocalizedMessage());
            return -1;
        }
    }

    @Override
    public boolean exists(Path file) {
        return Files.exists(file) && !Files.isDirectory(file);
    }

    @Override
    public long getUsage() {
        Mutable<Long> usage = new Mutable<>(0L);

        try {
            Files.walk(this.rootDirectory).forEach(path -> {
                if (Files.exists(path) && !Files.isDirectory(path)) {
                    try {
                        usage.set(usage.get() + Files.size(path));
                    } catch (IOException ignored) {
                    }
                }
            });
        } catch (IOException e) {
            usage.set(-1L);
        }

        return usage.get();
    }

    private static class FixedQuota extends FileSystemStorageProvider {
        private final long maxSize;

        /**
         * @param rootDirectory root directory of storage space
         * @param maxSize the largest amount of storage available for use, in bytes
         */
        private FixedQuota(Logger logger, Path rootDirectory, long maxSize) {
            super(logger, rootDirectory);
            this.maxSize = maxSize;

            if (maxSize <= 0) {
                throw new RuntimeException();
            }
        }

        @Override
        public boolean isFull() {
            return canHold(0);
        }

        @Override
        public boolean canHold(long contentLength) {
            return this.getUsage() + contentLength >= this.maxSize;
        }
    }

    private static class PercentageQuota extends FileSystemStorageProvider {
        private final double maxPercentage;

        /**
         * @param rootDirectory root directory of storage space
         * @param maxPercentage the maximum percentage of the disk available for use
         */
        private PercentageQuota(Logger logger, Path rootDirectory, double maxPercentage) {
            super(logger, rootDirectory);
            this.maxPercentage = maxPercentage;

            if (maxPercentage > 1 || maxPercentage <= 0) {
                throw new RuntimeException();
            }
        }

        @Override
        public boolean isFull() {
            return canHold(0);
        }

        @Override
        public boolean canHold(long contentLength) {
            try {
                return (this.getUsage() + contentLength) / (double) Files.getFileStore(this.rootDirectory).getUsableSpace() >= this.maxPercentage;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }
    }
}
