package com.reposilite.maven

import com.reposilite.maven.api.LatestArtifactQuery
import com.reposilite.maven.api.LatestArtifactQueryRequest
import com.reposilite.maven.api.LatestBadgeRequest
import com.reposilite.maven.api.LatestVersionResponse
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.shared.BadgeGenerator
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.toLocation
import com.reposilite.web.http.ErrorResponse
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

    fun <T> queryLatestArtifact(request: LatestArtifactQueryRequest, supplier: LatestVersionSupplier, handler: MatchedVersionHandler<T>): Result<T, ErrorResponse> =
        supplier.findLatestVersion(request.toVersionLookupRequest())
            .map { (isSnapshot, version) -> createLatestArtifactLocation(request.query, isSnapshot, version) }
            .flatMap { (version, fileLocation) -> matchLocation(request, supplier, handler, version, fileLocation) }

    private fun <T> matchLocation(request: LatestArtifactQueryRequest, supplier: LatestVersionSupplier, handler: MatchedVersionHandler<T>, version: String, fileLocation: Location) =
        handler
            .onMatch(LookupRequest(request.accessToken, request.repository.name, fileLocation))
            .flatMapErr {
                version
                    .takeIf { version.contains("-SNAPSHOT", ignoreCase = true) }
                    ?.let { matchSnapshotLocation(request, supplier, handler, version) }
                    ?: Result.error(it)
            }

    private fun <T> matchSnapshotLocation(request: LatestArtifactQueryRequest, supplier: LatestVersionSupplier, handler: MatchedVersionHandler<T>, version: String) =
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
