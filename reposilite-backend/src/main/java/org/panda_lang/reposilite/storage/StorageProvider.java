package org.panda_lang.reposilite.storage;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

public abstract class StorageProvider {
    protected final Logger logger;

    protected StorageProvider(Logger logger) {
        this.logger = logger;
    }

    abstract void putFile(Path file, byte[] bytes);

    abstract void putFile(Path file, byte[] bytes, String contentType);

    abstract byte[] getFile(Path file);

    abstract void removeFile(Path file);

    abstract List<Path> getFiles(Path directory);

    abstract FileTime getLastModifiedTime(Path file);

    abstract long getFileSize(Path file);

    abstract boolean exists(Path file);

    abstract boolean isFull();

    abstract long getUsage();

    abstract boolean canHold(long contentLength);
}
