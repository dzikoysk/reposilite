/*
 * Copyright (c) 2023 dzikoysk
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

import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.api.FileDetails
import panda.std.Result
import java.io.InputStream

enum class AuthenticationMethod {
    BASIC,
    CUSTOM_HEADER,
    LOOPBACK_LINK
}

interface RemoteCredentials {
    val method: AuthenticationMethod
    val login: String
    val password: String
}

interface RemoteClient {

    /**
     * @param uri - full remote host address with a gav
     * @param credentials - basic credentials in user:password format
     * @param connectTimeoutInSeconds - connection establishment timeout in seconds
     * @param readTimeoutInSeconds - connection read timeout in seconds
     */
    fun head(uri: String, credentials: RemoteCredentials?, connectTimeoutInSeconds: Int, readTimeoutInSeconds: Int): Result<out FileDetails, ErrorResponse>

    /**
     * @param uri - full remote host address with a gav
     * @param credentials - basic credentials in user:password format
     * @param connectTimeoutInSeconds - connection establishment timeout in seconds
     * @param readTimeoutInSeconds - connection read timeout in seconds
     */
    fun get(uri: String, credentials: RemoteCredentials?, connectTimeoutInSeconds: Int, readTimeoutInSeconds: Int): Result<InputStream, ErrorResponse>

}
