package org.panda_lang.reposilite.storage;

import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.repository.FileDetailsDto;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.function.Result;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

public class S3StorageProvider implements StorageProvider {
    private final S3Client s3;
    private final String bucket;

    public S3StorageProvider(String bucketName, String region) {
        this.bucket = bucketName;
        this.s3 = S3Client.builder().region(Region.of(region)).credentialsProvider(AnonymousCredentialsProvider.create()).build();
    }

    @Override
    public Result<FileDetailsDto, ErrorDto> putFile(Path file, byte[] bytes) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        builder.bucket(bucket);
        builder.key(file.toString().replace('\\', '/'));
        builder.contentType(FilesUtils.getMimeType(file.toString(), "text/plain"));
        builder.contentLength((long) bytes.length);

        try {
            this.s3.putObject(builder.build(), RequestBody.fromBytes(bytes));

            return Result.ok(new FileDetailsDto(
                    FileDetailsDto.FILE,
                    file.getFileName().toString(),
                    FileDetailsDto.DATE_FORMAT.format(System.currentTimeMillis()),
                    FilesUtils.getMimeType(file.getFileName().toString(), "application/octet-stream"),
                    bytes.length
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to write " + file.toString()));
        }
    }

    @Override
    public Result<FileDetailsDto, ErrorDto> putFile(Path file, InputStream inputStream) {
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder();
            builder.bucket(bucket);
            builder.key(file.toString().replace('\\', '/'));
            builder.contentType(FilesUtils.getMimeType(file.toString(), "text/plain"));

            long length = inputStream.available();
            builder.contentLength(length);

            this.s3.putObject(builder.build(), RequestBody.fromInputStream(inputStream, inputStream.available()));

            return Result.ok(new FileDetailsDto(
                    FileDetailsDto.FILE,
                    file.getFileName().toString(),
                    FileDetailsDto.DATE_FORMAT.format(System.currentTimeMillis()),
                    FilesUtils.getMimeType(file.getFileName().toString(), "application/octet-stream"),
                    length
            ));

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to write " + file.toString()));
        }
    }

    @Override
    public Result<byte[], ErrorDto> getFile(Path file) {
        try {
            GetObjectRequest.Builder request = GetObjectRequest.builder();

            request.bucket(this.bucket);
            request.key(file.toString().replace('\\', '/'));

            ResponseInputStream<GetObjectResponse> response = this.s3.getObject(request.build());

            byte[] bytes = new byte[Math.toIntExact(response.response().contentLength())];

            int read = response.read(bytes);

            return Result.ok(bytes);
        } catch (NoSuchKeyException | IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
        }
    }

    @Override
    public Result<FileDetailsDto, ErrorDto> getFileDetails(Path file) {
        if (file.toString().equals("")) {
            return Result.ok(new FileDetailsDto(
                    FileDetailsDto.DIRECTORY,
                    "",
                    "WHATEVER",
                    "application/octet-stream",
                    0
            ));
        }

        HeadObjectResponse response = this.head(file);

        if (response != null) {
            return Result.ok(new FileDetailsDto(
                    FileDetailsDto.FILE,
                    file.getFileName().toString(),
                    FileDetailsDto.DATE_FORMAT.format(System.currentTimeMillis()),
                    FilesUtils.getMimeType(file.getFileName().toString(), "application/octet-stream"),
                    response.contentLength()
            ));
        }

        return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
    }

    @Override
    public Result<Void, ErrorDto> removeFile(Path file) {
        DeleteObjectRequest.Builder request = DeleteObjectRequest.builder();

        request.bucket(this.bucket);
        request.key(file.toString().replace('\\', '/'));

        this.s3.deleteObject(request.build());

        return Result.ok(null);
    }

    @Override
    public Result<List<Path>, ErrorDto> getFiles(Path directory) {
        try {
            ListObjectsRequest.Builder request = ListObjectsRequest.builder();

            request.bucket(this.bucket);

            String directoryString = directory.toString().replace('\\', '/');
            request.prefix(directoryString);
//            request.delimiter("/");

            ListObjectsResponse response = this.s3.listObjects(request.build());

            List<Path> paths = new ArrayList<>();

            for (S3Object object : response.contents()) {
                String sub = object.key().substring(directoryString.length());

                paths.add(Paths.get(sub));
            }

            return Result.ok(paths);
        } catch (Exception e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }
    }

    @Override
    public Result<FileTime, ErrorDto> getLastModifiedTime(Path file) {
        HeadObjectResponse response = this.head(file);

        if (response != null) {
            return Result.ok(FileTime.from(response.lastModified()));
        } else {
            Result<List<Path>, ErrorDto> result = this.getFiles(file);

            if (result.isOk()) {
                for (Path path : result.get()) {
                    return this.getLastModifiedTime(file.resolve(path.getName(0)));
                }
            }

            return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
        }
    }

    @Override
    public Result<Long, ErrorDto> getFileSize(Path file) {
        HeadObjectResponse response = this.head(file);

        if (response != null) {
            return Result.ok(response.contentLength());
        } else {
            return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
        }
    }

    private @Nullable HeadObjectResponse head(Path file) {
        try {
            HeadObjectRequest.Builder request = HeadObjectRequest.builder();

            request.bucket(this.bucket);
            request.key(file.toString().replace('\\', '/'));

            return this.s3.headObject(request.build());
        } catch (NoSuchKeyException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean exists(Path file) {
        HeadObjectResponse response = this.head(file);

        return response != null;
    }

    @Override
    public boolean isDirectory(Path file) {
        Result<List<Path>, ErrorDto> files = this.getFiles(file);
        return files.isOk() && !files.get().isEmpty() && !this.exists(file);
    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public long getUsage() {
        // TODO
        return -1;
    }

    @Override
    public boolean canHold(long contentLength) {
        return true;
    }
}
