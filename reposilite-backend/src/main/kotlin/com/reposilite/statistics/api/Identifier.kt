package com.reposilite.statistics.api

import java.util.UUID

const val GAV_MAX_LENGTH = 1024

data class Identifier(
    val repository: String,
    val gav: String
) {

    init {
        if (gav.length > GAV_MAX_LENGTH) {
            throw IllegalStateException("Gav cannot exceed $GAV_MAX_LENGTH characters")
        }
    }

    fun toUUID(): UUID =
        UUID.nameUUIDFromBytes(toString().encodeToByteArray())

    override fun toString(): String =
        "/$repository/$gav"

}
