package com.reposilite.token.api

import com.reposilite.token.AccessTokenId
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.AccessTokenType
import com.reposilite.token.Route
import java.time.LocalDate

data class AccessTokenDto(
    val id: AccessTokenId,
    val type: AccessTokenType,
    val name: String,
    val createdAt: LocalDate,
    val description: String
)

data class AccessTokenDetails(
    private val accessTokenDto: AccessTokenDto,
    private val permissions: Set<AccessTokenPermission>,
    private val routes: Set<Route>
)