package org.panda_lang.reposilite.storage

import org.apache.http.HttpStatus
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.repository.api.FileDetailsResponse
import org.panda_lang.reposilite.utils.FilesUtils.getMimeType
import org.panda_lang.reposilite.utils.MimeTypes.MIME_OCTET_STREAM
import org.panda_lang.reposilite.utils.MimeTypes.MIME_PLAIN
import org.panda_lang.utilities.commons.function.Result
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime

internal class S3StorageProvider(private val bucket: String, region: String) : StorageProvider {

    private val s3: S3Client = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(AnonymousCredentialsProvider.create())
        .build()

    override fun putFile(file: Path, bytes: ByteArray): Result<FileDetailsResponse, ErrorResponse> {
        val builder = PutObjectRequest.builder()
        builder.bucket(bucket)
        builder.key(file.toString().replace('\\', '/'))
        builder.contentType(getMimeType(file.toString(), MIME_PLAIN))
        builder.contentLength(bytes.size.toLong())

        return try {
            s3.putObject(
                builder.build(),
                RequestBody.fromBytes(bytes)
            )

            Result.ok(
                FileDetailsResponse(
                    FileDetailsResponse.FILE,
                    file.fileName.toString(),
                    FileDetailsResponse.DATE_FORMAT.format(System.currentTimeMillis()),
                    getMimeType(file.fileName.toString(), MIME_OCTET_STREAM),
                    bytes.size.toLong()
                )
            )
        }
        catch (exception: Exception) {
            exception.printStackTrace()
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to write $file"))
        }
    }

    override fun putFile(file: Path, inputStream: InputStream): Result<FileDetailsResponse, ErrorResponse> {
        return try {
            val builder = PutObjectRequest.builder()
            builder.bucket(bucket)
            builder.key(file.toString().replace('\\', '/'))
            builder.contentType(getMimeType(file.toString(), MIME_PLAIN))

            val length = inputStream.available().toLong()
            builder.contentLength(length)

            s3.putObject(
                builder.build(),
                RequestBody.fromInputStream(inputStream, inputStream.available().toLong())
            )

            Result.ok(
                FileDetailsResponse(
                    FileDetailsResponse.FILE,
                    file.fileName.toString(),
                    FileDetailsResponse.DATE_FORMAT.format(System.currentTimeMillis()),
                    getMimeType(file.fileName.toString(), MIME_OCTET_STREAM),
                    length
                )
            )
        }
        catch (ioException: IOException) {
            ioException.printStackTrace()
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to write $file"))
        }
    }

    override fun getFile(file: Path): Result<ByteArray, ErrorResponse> {
        return try {
            val request = GetObjectRequest.builder()
            request.bucket(bucket)
            request.key(file.toString().replace('\\', '/'))

            val response = s3.getObject(request.build())
            val bytes = ByteArray(Math.toIntExact(response.response().contentLength()))
            val read = response.read(bytes)
            // TODO: verify - read not used?

            Result.ok(bytes)
        }
        catch (noSuchKeyException: NoSuchKeyException) {
            Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "File not found: $file"))
        }
        catch (ioException: IOException) {
            Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "File not found: $file"))
        }
    }

    override fun getFileDetails(file: Path): Result<FileDetailsResponse, ErrorResponse> {
        if (file.toString() == "") {
            return Result.ok(
                FileDetailsResponse(
                    FileDetailsResponse.DIRECTORY,
                    "",
                    "WHATEVER",
                    "application/octet-stream",
                    0
                )
            )
        }

        val response = head(file)

        return if (response != null) {
            Result.ok(
                FileDetailsResponse(
                    FileDetailsResponse.FILE,
                    file.fileName.toString(),
                    FileDetailsResponse.DATE_FORMAT.format(System.currentTimeMillis()),
                    getMimeType(file.fileName.toString(), "application/octet-stream"),
                    response.contentLength()
                )
            )
        }
        else Result.error(
            ErrorResponse(
                HttpStatus.SC_NOT_FOUND,
                "File not found: $file"
            )
        )
    }

    override fun removeFile(file: Path): Result<Void, ErrorResponse> {
        val request = DeleteObjectRequest.builder()
        request.bucket(bucket)
        request.key(file.toString().replace('\\', '/'))
        s3.deleteObject(request.build())
        return Result.ok(null)
    }

    override fun getFiles(directory: Path): Result<List<Path>, ErrorResponse> {
        return try {
            val request = ListObjectsRequest.builder()
            request.bucket(bucket)

            val directoryString = directory.toString().replace('\\', '/')
            request.prefix(directoryString)
            //            request.delimiter("/");

            val response = s3.listObjects(request.build())
            val paths: MutableList<Path> = ArrayList()

            for (content in response.contents()) {
                val sub = content.key().substring(directoryString.length)
                paths.add(Paths.get(sub))
            }

            Result.ok(paths)
        }
        catch (exception: Exception) {
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, exception.localizedMessage))
        }
    }

    override fun getLastModifiedTime(file: Path): Result<FileTime, ErrorResponse> {
        val response = head(file)

        return if (response != null) {
            Result.ok(FileTime.from(response.lastModified()))
        } else {
            val result = getFiles(file)

            if (result.isOk) {
                for (path in result.get()) {
                    return getLastModifiedTime(file.resolve(path.getName(0)))
                }
            }

            Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "File not found: $file"))
        }
    }

    override fun getFileSize(file: Path): Result<Long, ErrorResponse> {
        val response = head(file)

        return if (response != null) {
            Result.ok(response.contentLength())
        } else {
            Result.error(
                ErrorResponse(
                    HttpStatus.SC_NOT_FOUND,
                    "File not found: $file"
                )
            )
        }
    }

    private fun head(file: Path): HeadObjectResponse? {
        try {
            val request = HeadObjectRequest.builder()
            request.bucket(bucket)
            request.key(file.toString().replace('\\', '/'))

            return s3.headObject(request.build())
        }
        catch (ignored: NoSuchKeyException) {
            // ignored
        }
        catch (exception: Exception) {
            exception.printStackTrace()
        }

        return null
    }

    override fun exists(file: Path): Boolean {
        val response = head(file)
        return response != null
    }

    override fun isDirectory(file: Path): Boolean {
        val files = getFiles(file)
        return files.isOk && files.get().isNotEmpty() && !exists(file)
    }

    override val isFull: Boolean
        get() = false

    // TODO
    override val usage: Long
        get() =// TODO
            -1

    override fun canHold(contentLength: Long): Boolean {
        return true
    }

}