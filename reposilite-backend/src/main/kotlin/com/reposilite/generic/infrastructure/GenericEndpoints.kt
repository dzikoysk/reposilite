/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.generic.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.generic.GenericFacade
import com.reposilite.generic.GenericRepository
import com.reposilite.repository.infrastructure.createDirectoryIndexPage
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.extensions.resultAttachment
import com.reposilite.shared.extensions.uri
import com.reposilite.shared.notFoundError
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.DELETE
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.Route.HEAD
import io.javalin.community.routing.Route.POST
import io.javalin.community.routing.Route.PUT
import io.javalin.http.HandlerType.HEAD as HEAD_METHOD
import io.javalin.http.Context
import panda.std.Result
import panda.std.asError
import panda.std.asSuccess
import java.io.InputStream

internal class GenericEndpoints(
    private val genericFacade: GenericFacade,
    private val frontendFacade: FrontendFacade,
    private val compressionStrategy: String,
) : ReposiliteRoutes() {

    private val browseRepositoryRoot = ReposiliteRoute<Unit>("/{repository}", HEAD, GET) {
        accessed {
            requireRepository { repository ->
                findFile(ctx, this?.identifier, repository, Location.empty())
            }
        }
    }

    private val browseRepositoryPath = ReposiliteRoute<Unit>("/{repository}/<path>", HEAD, GET) {
        accessed {
            requireRepository { repository ->
                requireLocation { location ->
                    findFile(ctx, this?.identifier, repository, location)
                }
            }
        }
    }

    private fun findFile(
        ctx: Context,
        identifier: AccessTokenIdentifier?,
        repository: GenericRepository,
        location: Location,
    ): Result<Unit, ErrorResponse> =
        genericFacade.findDetails(identifier, repository, location)
            .flatMap { details ->
                when (details) {
                    is DocumentInfo ->
                        when (ctx.method()) {
                            HEAD_METHOD -> InputStream.nullInputStream().asSuccess()
                            else -> genericFacade.findData(identifier, repository, location)
                        }.map { data ->
                            ctx.resultAttachment(
                                name = details.name,
                                contentType = details.contentType,
                                contentLength = details.contentLength,
                                lastTimeModified = details.lastModifiedTime,
                                compressionStrategy = compressionStrategy,
                                cache = false,
                                data = data,
                            )
                        }
                    is DirectoryInfo -> {
                        ctx.html(
                            createDirectoryIndexPage(
                                basePath = frontendFacade.resolveBasePath(ctx.header(frontendFacade.forwardedPrefixHeader.get())),
                                uri = ctx.uri(),
                                visibleFiles = genericFacade.getAvailableFiles(identifier, repository, location, details),
                            )
                        )
                        Unit.asSuccess()
                    }
                    else -> throw IllegalStateException("Expected file details, but got $details")
                }
            }
            .onError { error ->
                ctx.status(error.status).html(
                    frontendFacade.createNotFoundPage(
                        originUri = ctx.uri(),
                        details = error.message,
                        forwardedPrefix = ctx.header(frontendFacade.forwardedPrefixHeader.get()),
                    )
                )
            }

    private val deployFile = ReposiliteRoute<Unit>("/{repository}/<path>", POST, PUT) {
        authorized {
            requireRepository { repository ->
                requireLocation { location ->
                    response = genericFacade.deployFile(identifier, repository, location, ctx.bodyInputStream(), getSessionIdentifier())
                }
            }
        }
    }

    private val deleteFile = ReposiliteRoute<Unit>("/{repository}/<path>", DELETE) {
        authorized {
            requireRepository { repository ->
                requireLocation { location ->
                    response = genericFacade.deleteFile(identifier, repository, location, getSessionIdentifier())
                }
            }
        }
    }

    override val routes = routes(browseRepositoryRoot, browseRepositoryPath, deployFile, deleteFile)

    private fun <R> ContextDsl<R>.requireRepository(block: (GenericRepository) -> Unit) {
        genericFacade.getRepository(requireParameter("repository"))
            ?.let(block)
            ?: run { response = notFoundError("Repository not found") }
    }

    private fun <R> ContextDsl<R>.requireLocation(block: (Location) -> Unit) {
        Location.ofRequest(requireParameter("path"))
            .peek(block)
            .onError { response = it.asError() }
    }
}
