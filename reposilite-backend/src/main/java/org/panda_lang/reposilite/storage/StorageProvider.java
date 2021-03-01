package org.panda_lang.reposilite.storage;

import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.utilities.commons.function.Result;

import java.io.IOException;
import java.nio.file.Path;

public interface StorageProvider {
    Result<Boolean, ErrorDto> putFile(Path file, byte[] bytes) throws IOException;

    Result<Boolean, ErrorDto> putFile(Path file, byte[] bytes, String contentType) throws IOException;
}
