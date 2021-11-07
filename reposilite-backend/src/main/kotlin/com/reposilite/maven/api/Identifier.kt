package com.reposilite.maven.api

import java.util.UUID

const val REPOSITORY_NAME_MAX_LENGTH = 32
const val GAV_MAX_LENGTH = 1024

/**
 * Gav is a string that represents path in associated repository.
 * The name is a short for Group:Artifact:Version, but it real-world usage it represents all entries within the specified repository.
 */
data class Identifier(val repository: String, val gav: String) {

    init {
        if (repository.length > REPOSITORY_NAME_MAX_LENGTH) {
            throw IllegalStateException("Repository name cannot exceed $GAV_MAX_LENGTH characters")
        }

        if (gav.length > GAV_MAX_LENGTH) {
            throw IllegalStateException("Gav cannot exceed $GAV_MAX_LENGTH characters")
        }
    }

    fun toUUID(): UUID =
        UUID.nameUUIDFromBytes(toString().encodeToByteArray())

    override fun toString(): String =
        "/$repository/$gav"

}
