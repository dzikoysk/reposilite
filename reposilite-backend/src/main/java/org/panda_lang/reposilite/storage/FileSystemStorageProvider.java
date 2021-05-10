package org.panda_lang.reposilite.storage;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.repository.FileDetailsDto;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.function.Result;
import org.panda_lang.utilities.commons.function.ThrowingBiFunction;
import org.panda_lang.utilities.commons.function.ThrowingFunction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public abstract class FileSystemStorageProvider implements StorageProvider {
    protected final Path rootDirectory;

    public static FileSystemStorageProvider of(Path rootDirectory, String quota) {
        if (quota.endsWith("%")) {
            return of(rootDirectory, Integer.parseInt(quota.substring(0, quota.length() - 1)) / 100D);
        } else {
            return of(rootDirectory, FilesUtils.displaySizeToBytesCount(quota));
        }
    }

    /**
     * @param rootDirectory root directory of storage space
     * @param maxSize the largest amount of storage available for use, in bytes
     */
    public static FileSystemStorageProvider of(Path rootDirectory, long maxSize) {
        return new FixedQuota(rootDirectory, maxSize);
    }

    /**
     * @param rootDirectory root directory of storage space
     * @param maxPercentage the maximum percentage of the disk available for use
     */
    public static FileSystemStorageProvider of(Path rootDirectory, double maxPercentage) {
        return new PercentageQuota(rootDirectory, maxPercentage);
    }

    /**
     * @param rootDirectory root directory of storage space
     */
    private FileSystemStorageProvider(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public Result<FileDetailsDto, ErrorDto> putFile(Path file, byte[] bytes) {
        return this.putFile(file, bytes, b -> b.length, (in, out) -> out.write(ByteBuffer.wrap(in)));
    }

    @Override
    public Result<FileDetailsDto, ErrorDto> putFile(Path file, InputStream inputStream) {
        return this.putFile(file, inputStream, InputStream::available, (in, out) -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            byte[] byteArray = buffer.toByteArray();
            out.write(ByteBuffer.wrap(byteArray));

            return byteArray.length;
        });
    }

    private <T> Result<FileDetailsDto, ErrorDto> putFile(Path file, T input, ThrowingFunction<T, Integer, IOException> measure, ThrowingBiFunction<T, FileChannel, Integer, IOException> writer) {
        try {
            long size = measure.apply(input);

            if (!this.canHold(size)) {
                return Result.error(new ErrorDto(HttpStatus.SC_INSUFFICIENT_STORAGE, "Not enough storage space available"));
            }

            if (file.getParent() != null && !Files.exists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }

            if (!Files.exists(file)) {
                Files.createFile(file);
            }

            FileChannel fileChannel = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            fileChannel.lock();

            long bytesWritten = writer.apply(input, fileChannel);

            fileChannel.close();

            return Result.ok(new FileDetailsDto(FileDetailsDto.FILE,
                    file.getFileName().toString(),
                    FileDetailsDto.DATE_FORMAT.format(System.currentTimeMillis()),
                    FilesUtils.getMimeType(file.toString(), "application/octet-stream"),
                    bytesWritten
            ));
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }
    }

    @Override
    public Result<byte[], ErrorDto> getFile(Path file) {
        if (!Files.exists(file) || Files.isDirectory(file)) {
            return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
        }

        try {
            return Result.ok(Files.readAllBytes(file));
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }
    }

    @Override
    public Result<FileDetailsDto, ErrorDto> getFileDetails(Path file) {
        if (!Files.exists(file)) {
            return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
        }

        try {
            return Result.ok(new FileDetailsDto(
                    Files.isDirectory(file) ? FileDetailsDto.DIRECTORY : FileDetailsDto.FILE,
                    file.getFileName().toString(),
                    FileDetailsDto.DATE_FORMAT.format(Files.getLastModifiedTime(file).toMillis()),
                    FilesUtils.getMimeType(file.getFileName().toString(), "application/octet-stream"),
                    Files.size(file)
            ));
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }
    }

    @Override
    public Result<Void, ErrorDto> removeFile(Path file) {
        try {
            if (!Files.exists(file)) {
                return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
            }

            Files.delete(file);
            return Result.ok(null);
        } catch (IOException e) {
            return Result.error(new ErrorDto(500, e.getLocalizedMessage()));
        }
    }

    @Override
    public Result<List<Path>, ErrorDto> getFiles(Path directory) {
        try {
            return Result.ok(Files.walk(directory, 1).filter(path -> !path.equals(directory)).collect(Collectors.toList()));
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }
    }

    @Override
    public Result<FileTime, ErrorDto> getLastModifiedTime(Path file) {
        try {
            if (!Files.exists(file)) {
                return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
            }

            return Result.ok(Files.getLastModifiedTime(file));
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }
    }

    @Override
    public Result<Long, ErrorDto> getFileSize(Path file) {
        try {
            if (!Files.exists(file)) {
                return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
            }

            long size = 0;

            if (Files.isDirectory(file)) {
                for (Path path : Files.walk(file).collect(Collectors.toList())) {
                    size += Files.size(path);
                }
            } else {
                size = Files.size(file);
            }

            return Result.ok(size);
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }
    }

    @Override
    public boolean exists(Path file) {
        return Files.exists(file) && !Files.isDirectory(file);
    }

    @Override
    public boolean isDirectory(Path file) {
        return Files.isDirectory(file);
    }

    @Override
    public long getUsage() {
        AtomicLong usage = new AtomicLong();

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

    @Override
    public void shutdown() {
    }

    private static class FixedQuota extends FileSystemStorageProvider {
        private final long maxSize;

        /**
         * @param rootDirectory root directory of storage space
         * @param maxSize the largest amount of storage available for use, in bytes
         */
        private FixedQuota(Path rootDirectory, long maxSize) {
            super(rootDirectory);
            this.maxSize = maxSize;

            if (maxSize <= 0) {
                throw new RuntimeException();
            }
        }

        @Override
        public boolean isFull() {
            return !canHold(0);
        }

        @Override
        public boolean canHold(long contentLength) {
            return this.getUsage() + contentLength < this.maxSize;
        }
    }

    private static class PercentageQuota extends FileSystemStorageProvider {
        private final double maxPercentage;

        /**
         * @param rootDirectory root directory of storage space
         * @param maxPercentage the maximum percentage of the disk available for use
         */
        private PercentageQuota(Path rootDirectory, double maxPercentage) {
            super(rootDirectory);
            this.maxPercentage = maxPercentage;

            if (maxPercentage > 1 || maxPercentage <= 0) {
                throw new RuntimeException();
            }
        }

        @Override
        public boolean isFull() {
            return !canHold(0);
        }

        @Override
        public boolean canHold(long contentLength) {
            try {
                long newUsage = this.getUsage() + contentLength;
                double capacity = Files.getFileStore(this.rootDirectory).getUsableSpace();
                double percentage = newUsage / capacity;
                return percentage < this.maxPercentage;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }
    }
}
