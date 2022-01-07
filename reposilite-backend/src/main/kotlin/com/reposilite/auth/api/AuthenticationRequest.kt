package com.reposilite.auth.api

data class AuthenticationRequest(
    val name: String,
    val secret: String
)
