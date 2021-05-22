/*
 * Copyright (c) 2021 dzikoysk
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
package org.panda_lang.reposilite.maven.repository

import org.apache.http.HttpStatus
import org.panda_lang.reposilite.web.ReposiliteUtils.normalizeUri
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.auth.Authenticator
import org.panda_lang.reposilite.auth.Session
import org.panda_lang.reposilite.failure.ResponseUtils
import org.panda_lang.reposilite.maven.repository.api.FileListResponse
import org.panda_lang.reposilite.maven.repository.api.FileDetailsResponse
import org.panda_lang.utilities.commons.StringUtils
import org.panda_lang.utilities.commons.collection.Pair
import org.panda_lang.utilities.commons.function.Option
import org.panda_lang.utilities.commons.function.Result
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Function
import java.util.stream.Collectors

class RepositoryAuthenticator(
    private val rewritePathsEnabled: Boolean,
    private val authenticator: Authenticator,
    private val repositoryService: RepositoryService
) {

    fun authDefaultRepository(headers: Map<String?, String?>?, uri: String?): Result<Pair<Path?, Repository?>?, ErrorResponse?> {
        return normalizeUri(uri!!)
            .map { normalizedUri: String? -> authRepository(headers, normalizedUri) }
            .orElseGet { ResponseUtils.error(HttpStatus.SC_BAD_REQUEST, "Invalid GAV path") }
    }

    fun authRepository(headers: Map<String?, String?>?, normalizedUri: String?): Result<Pair<Path?, Repository?>?, ErrorResponse?> {
        val split = StringUtils.split(normalizedUri, "/")
        val repositoryName = split[0]
        if (StringUtils.isEmpty(repositoryName)) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Unsupported request")
        }
        var repository = repositoryService.getRepository(repositoryName)
        if (repository == null) {
            repository = if (rewritePathsEnabled) {
                repositoryService.primaryRepository
            } else {
                return ResponseUtils.error(
                    HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION,
                    "Repository $repositoryName not found"
                )
            }
        }

        // auth hidden repositories
        if (repository!!.isPrivate()) {
            val authResult: Result<Session, String> = authenticator.authByUri(headers, normalizedUri!!)
            if (authResult.isErr) {
                return ResponseUtils.error(HttpStatus.SC_UNAUTHORIZED, "Unauthorized request")
            }
        }
        var path = Paths.get(if (split.size > 1) split[1] else "")
        for (i in 2 until split.size) {
            path = path.resolve(split[i])
        }
        return Result.ok(Pair(path, repository))
    }

    fun findAvailableRepositories(headers: Map<String?, String?>?): FileListResponse<FileDetailsResponse> {
        val session: Option<Session> = authenticator.authByHeader(headers).toOption()

        return FileListResponse(
            repositoryService.repositories.stream()
                .filter { repository: Repository ->
                    repository.isPublic() || session.map(
                        Function<Session, Any> { value: Session -> value.getRepositoryNames().contains(repository.name) }).orElseGet(false)
                }
                .map { repository: Repository -> FileDetailsResponse(FileDetailsResponse.DIRECTORY, repository.name, "", "application/octet-stream", 0) }
                .collect(Collectors.toList()))
    }

}