package org.panda_lang.reposilite.storage;

import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemStorageProvider implements StorageProvider {
    private final Path rootDirectory;

    public FileSystemStorageProvider(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public Result<Boolean, ErrorDto> putFile(Path file, byte[] bytes) throws IOException {
        Path destination = this.rootDirectory.resolve(file);

        if (destination.getParent() != null && !Files.exists(destination.getParent())) {
            Files.createDirectories(destination.getParent());
        }

        Files.write(file, bytes);

        return Result.ok(true);
    }

    @Override
    public Result<Boolean, ErrorDto> putFile(Path file, byte[] bytes, String contentType) throws IOException {
        return this.putFile(file, bytes);
    }
}
