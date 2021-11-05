package com.reposilite.statistics.api

import java.util.UUID

data class Identifier(
    val repository: String,
    val gav: String
) {

    fun toUUID(): UUID =
        UUID.nameUUIDFromBytes(toString().encodeToByteArray())

    override fun toString(): String =
        "/$repository/$gav"

}
