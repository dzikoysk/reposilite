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

package com.reposilite.shared.http

import com.reposilite.journalist.Journalist
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.storage.api.FileDetails
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import java.io.InputStream
import java.net.Proxy

private typealias HeadHandler = (String, RepositorySettings.ProxiedRepository.Authorization?, Int, Int) -> Result<FileDetails, ErrorResponse>
private typealias GetHandler = (String, RepositorySettings.ProxiedRepository.Authorization?, Int, Int) -> Result<InputStream, ErrorResponse>

class FakeRemoteClientProvider(private val headHandler: HeadHandler, private val getHandler: GetHandler) : RemoteClientProvider {

    override fun createClient(journalist: Journalist, proxy: Proxy?): RemoteClient =
        FakeRemoteClient(headHandler, getHandler)

}

class FakeRemoteClient(private val headHandler: HeadHandler, private val getHandler: GetHandler) : RemoteClient {

    override fun head(uri: String, credentials: RepositorySettings.ProxiedRepository.Authorization?, connectTimeout: Int, readTimeout: Int): Result<FileDetails, ErrorResponse> =
        headHandler(uri, credentials, connectTimeout, readTimeout)

    override fun get(uri: String, credentials: RepositorySettings.ProxiedRepository.Authorization?, connectTimeout: Int, readTimeout: Int): Result<InputStream, ErrorResponse> =
        getHandler(uri, credentials, connectTimeout, readTimeout)

}
