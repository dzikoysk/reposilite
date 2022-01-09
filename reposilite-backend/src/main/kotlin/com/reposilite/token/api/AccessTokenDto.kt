package com.reposilite.token.api

import com.reposilite.token.AccessTokenIdentifier
import java.time.LocalDate

data class AccessTokenDto(
    val identifier: AccessTokenIdentifier,
    val name: String,
    val createdAt: LocalDate,
    val description: String
)