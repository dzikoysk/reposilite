package org.panda_lang.reposilite.storage;

import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.repository.FileDetailsDto;
import org.panda_lang.utilities.commons.function.Result;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

public interface StorageProvider {
    /**
     * Writes the bytes to the path specified in storage.
     *
     * @param file  the path of the file to be written
     * @param bytes the bytes to write
     * @return a {@link FileDetailsDto} object describing the file if successful, and an {@link ErrorDto} if not
     */
    Result<FileDetailsDto, ErrorDto> putFile(Path file, byte[] bytes);

    /**
     * Writes the given {@link InputStream} to the path specified in storage.
     *
     * @param file  the path of the file to be written
     * @param inputStream the stream supplying the data to write
     * @return a {@link FileDetailsDto} object describing the file if successful, and an {@link ErrorDto} if not
     */
    Result<FileDetailsDto, ErrorDto> putFile(Path file, InputStream inputStream);

    Result<byte[], ErrorDto> getFile(Path file);

    Result<FileDetailsDto, ErrorDto> getFileDetails(Path file);

    Result<Void, ErrorDto> removeFile(Path file);

    Result<List<Path>, ErrorDto> getFiles(Path directory);

    Result<FileTime, ErrorDto> getLastModifiedTime(Path file);

    Result<Long, ErrorDto> getFileSize(Path file);

    boolean exists(Path file);

    boolean isDirectory(Path file);

    boolean isFull();

    long getUsage();

    boolean canHold(long contentLength);

    default void shutdown() {

    }
}
