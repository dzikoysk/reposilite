package com.reposilite.maven

import com.reposilite.shared.http.RemoteClient
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.toLocation
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFound
import panda.std.Result
import java.io.InputStream

internal class RepositoryLoopbackClient(private val repository: Lazy<Repository>) : RemoteClient {

    override fun head(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<FileDetails, ErrorResponse> =
        repository.value.getFileDetails(toGav(uri))
            .`is`(FileDetails::class.java) { notFound("Requested file is a directory") }

    override fun get(uri: String, credentials: String?, connectTimeout: Int, readTimeout: Int): Result<InputStream, ErrorResponse> =
        repository.value.getFile(toGav(uri))

    private fun toGav(uri: String): Location =
        uri.substring(repository.value.name.length + 1).toLocation()

}