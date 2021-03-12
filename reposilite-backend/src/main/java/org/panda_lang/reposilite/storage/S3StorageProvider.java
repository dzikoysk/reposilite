package org.panda_lang.reposilite.storage;

import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.repository.FileDetailsDto;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.function.Result;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
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
    private final S3Client s3 = S3Client.create();
    private final String bucket;

    public S3StorageProvider(String bucketName) {
        this.bucket = bucketName;
    }

    @Override
    public Result<FileDetailsDto, ErrorDto> putFile(Path file, byte[] bytes) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        builder.bucket(bucket);
        builder.key(file.toString());
        builder.contentType(FilesUtils.getMimeType(file.toString(), "text/plain"));
        builder.contentLength((long) bytes.length);

        PutObjectResponse response = this.s3.putObject(builder.build(), RequestBody.fromBytes(bytes));

        return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "S3 file test"));
    }

    @Override
    public Result<FileDetailsDto, ErrorDto> putFile(Path file, InputStream inputStream) {
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder();
            builder.bucket(bucket);
            builder.key(file.toString());
            builder.contentType(FilesUtils.getMimeType(file.toString(), "text/plain"));
            builder.contentLength((long) inputStream.available());

            PutObjectResponse response = this.s3.putObject(builder.build(), RequestBody.fromInputStream(inputStream, inputStream.available()));

            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "S3 file test"));
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "S3 file test 2"));
        }
    }

    @Override
    public Result<byte[], ErrorDto> getFile(Path file) {
        try {
            GetObjectRequest.Builder request = GetObjectRequest.builder();

            request.bucket(this.bucket);
            request.key(file.toString());

            ResponseInputStream<GetObjectResponse> response = this.s3.getObject(request.build());

            byte[] bytes = new byte[Math.toIntExact(response.response().contentLength())];

            int written = response.read(bytes);

            return Result.ok(bytes);
        } catch (IOException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, "S3 file test"));
        }
    }

    @Override
    public Result<FileDetailsDto, ErrorDto> getFileDetails(Path file) {
        try {
            HeadObjectResponse response = this.head(file);

            return Result.ok(new FileDetailsDto(
                    FileDetailsDto.FILE,
                    file.getFileName().toString(),
                    FileDetailsDto.DATE_FORMAT.format(response.lastModified()),
                    FilesUtils.getMimeType(file.getFileName().toString(), "application/octet-stream"),
                    response.contentLength()
            ));
        } catch (NoSuchKeyException e) {
            return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "File not found: " + file.toString()));
        }
    }

    @Override
    public Result<Void, ErrorDto> removeFile(Path file) {
        DeleteObjectRequest.Builder request = DeleteObjectRequest.builder();

        request.bucket(this.bucket);
        request.key(file.toString());

        this.s3.deleteObject(request.build());

        return Result.ok(null);
    }

    @Override
    public Result<List<Path>, ErrorDto> getFiles(Path directory) {
        try {
            ListObjectsRequest.Builder request = ListObjectsRequest.builder();

            request.bucket(this.bucket);

            String directoryString = directory.toString();
            request.prefix(directoryString);
            request.delimiter("/");

            ListObjectsResponse response = this.s3.listObjects(request.build());

            List<Path> paths = new ArrayList<>();

            for (S3Object object : response.contents()) {
                String sub = object.key().substring(directoryString.length());

                if (sub.chars().filter(c -> c == '/').count() == 1) {
                    paths.add(Paths.get(sub));
                }
            }

            return Result.ok(paths);
        } catch (Exception e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }
    }

    @Override
    public Result<FileTime, ErrorDto> getLastModifiedTime(Path file) {
        try {
            return Result.ok(FileTime.from(this.head(file).lastModified()));
        } catch (Exception e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }    }

    @Override
    public Result<Long, ErrorDto> getFileSize(Path file) {
        try {
            return Result.ok(this.head(file).contentLength());
        } catch (Exception e) {
            return Result.error(new ErrorDto(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()));
        }
    }

    private HeadObjectResponse head(Path file) {
        HeadObjectRequest.Builder request = HeadObjectRequest.builder();

        request.bucket(this.bucket);
        request.key(file.toString());

        return this.s3.headObject(request.build());
    }

    @Override
    public boolean exists(Path file) {
        try {
            return this.head(file) != null;
        } catch (Exception e) {
            return false;
        }
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
        return -1;
    }

    @Override
    public boolean canHold(long contentLength) {
        return true;
    }
}
