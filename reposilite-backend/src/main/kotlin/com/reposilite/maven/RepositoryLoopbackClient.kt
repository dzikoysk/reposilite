/*
 * Copyright (c) 2022 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.maven

import com.reposilite.auth.api.Credentials
import com.reposilite.maven.application.ProxiedRepository
import com.reposilite.shared.http.RemoteClient
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.toLocation
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFound
import panda.std.Result
import java.io.InputStream

internal class RepositoryLoopbackClient(private val repository: Lazy<Repository>) : RemoteClient {

    override fun head(uri: String, credentials: Credentials?, connectTimeout: Int, readTimeout: Int): Result<FileDetails, ErrorResponse> =
        repository.value.getFileDetails(toGav(uri))
            .`is`(FileDetails::class.java) { notFound("Requested file is a directory") }

    override fun get(uri: String, credentials: Credentials?, connectTimeout: Int, readTimeout: Int): Result<InputStream, ErrorResponse> =
        repository.value.getFile(toGav(uri))

    private fun toGav(uri: String): Location =
        uri.substring(repository.value.name.length + 1).toLocation()

}
