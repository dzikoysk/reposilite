package com.reposilite.auth.api

import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.Route
import com.reposilite.token.api.AccessTokenDto

data class SessionDetails(
    val accessToken: AccessTokenDto,
    val permissions: Set<AccessTokenPermission>,
    val routes: Set<Route>
)