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

package com.reposilite.packages.maven

import com.reposilite.packages.maven.api.LatestArtifactQuery
import com.reposilite.packages.maven.api.LatestArtifactQueryRequest
import com.reposilite.packages.maven.api.LatestBadgeRequest
import com.reposilite.packages.maven.api.LatestVersionResponse
import com.reposilite.packages.maven.api.LookupRequest
import com.reposilite.packages.maven.api.VersionLookupRequest
import com.reposilite.shared.BadgeGenerator
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.toLocation
import panda.std.Result
import panda.std.reactive.Reference

fun interface MatchedVersionHandler<T> {
    fun onMatch(request: LookupRequest): Result<T, ErrorResponse>
}

fun interface LatestVersionSupplier {
    fun findLatestVersion(request: VersionLookupRequest): Result<LatestVersionResponse, ErrorResponse>
}

internal class LatestService(private val repositoryId: Reference<out String>) {

    fun createLatestBadge(request: LatestBadgeRequest, version: String): Result<String, ErrorResponse> =
        BadgeGenerator.generateSvg(
            name = request.name ?: repositoryId.get(),
            value = (request.prefix ?: "") + version,
            optionalColor = request.color
        )

    fun <T> queryLatestArtifact(request: LatestArtifactQueryRequest, supplier: com.reposilite.packages.maven.LatestVersionSupplier, handler: com.reposilite.packages.maven.MatchedVersionHandler<T>): Result<T, ErrorResponse> =
        supplier.findLatestVersion(request.toVersionLookupRequest())
            .map { (isSnapshot, version) -> createLatestArtifactLocation(request.query, isSnapshot, version) }
            .flatMap { (version, fileLocation) -> matchLocation(request, supplier, handler, version, fileLocation) }

    private fun <T> matchLocation(request: LatestArtifactQueryRequest, supplier: com.reposilite.packages.maven.LatestVersionSupplier, handler: com.reposilite.packages.maven.MatchedVersionHandler<T>, version: String, fileLocation: Location) =
        handler
            .onMatch(LookupRequest(request.accessToken, request.mavenRepository.name, fileLocation))
            .flatMapErr {
                version
                    .takeIf { version.contains("-SNAPSHOT", ignoreCase = true) }
                    ?.let { matchSnapshotLocation(request, supplier, handler, version) }
                    ?: Result.error(it)
            }

    private fun <T> matchSnapshotLocation(request: LatestArtifactQueryRequest, supplier: com.reposilite.packages.maven.LatestVersionSupplier, handler: com.reposilite.packages.maven.MatchedVersionHandler<T>, version: String) =
        queryLatestArtifact(
            request = request.copy(
                query = request.query.copy(
                    gav = "${request.query.gav}/$version".toLocation()
                )
            ),
            supplier = supplier,
            handler = handler
        )

    private fun createLatestArtifactLocation(query: LatestArtifactQuery, isSnapshot: Boolean, version: String): Pair<String, Location> =
        with(query) {
            val suffix = version + (if (classifier != null) "-$classifier" else "") + "." + extension

            if (isSnapshot)
                version to "$gav/${gav.locationBeforeLast("/", "").locationAfterLast("/", "")}-$suffix".toLocation()
            else
                version to "$gav/$version/${gav.locationAfterLast("/", "")}-$suffix".toLocation()
        }

}
