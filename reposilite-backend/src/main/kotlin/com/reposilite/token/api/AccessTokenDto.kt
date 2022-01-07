package com.reposilite.token.api

import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.Route
import java.time.LocalDate

data class AccessTokenDto(
    val identifier: AccessTokenIdentifier,
    val name: String,
    val createdAt: LocalDate,
    val description: String
)

data class AccessTokenDetails(
    val accessToken: AccessTokenDto,
    val permissions: Set<AccessTokenPermission>,
    val routes: Set<Route>
)