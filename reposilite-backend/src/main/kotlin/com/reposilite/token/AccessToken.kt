/*
 * Copyright (c) 2022 dzikoysk
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
package com.reposilite.token

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.application.AccessTokenPlugin.Companion.MAX_TOKEN_NAME
import net.dzikoysk.exposed.shared.UNINITIALIZED_ENTITY_ID
import java.time.LocalDate

enum class AccessTokenType {
    PERSISTENT,
    TEMPORARY
}

data class AccessTokenIdentifier(
    val type: AccessTokenType = PERSISTENT,
    val value: Int = UNINITIALIZED_ENTITY_ID,
)

internal data class AccessToken(
    val identifier: AccessTokenIdentifier = AccessTokenIdentifier(),
    val name: String,
    val encryptedSecret: String = "",
    val createdAt: LocalDate = LocalDate.now(),
    val description: String = "",
) {

    init {
        if (name.length > MAX_TOKEN_NAME) {
            throw IllegalStateException("Name is too long (${name.length} > $MAX_TOKEN_NAME)")
        }
    }

    fun toDto(): AccessTokenDto =
        AccessTokenDto(
            identifier = identifier,
            name = name,
            createdAt = createdAt,
            description = description
        )

}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class AccessTokenPermission(val identifier: String, val shortcut: String) {

    MANAGER("access-token:manager", "m");

    companion object {

        fun findAccessTokenPermissionByIdentifier(identifier: String): AccessTokenPermission? =
            values().firstOrNull { it.identifier == identifier }

        fun findAccessTokenPermissionByShortcut(shortcut: String): AccessTokenPermission? =
            values().firstOrNull { it.shortcut == shortcut }

        fun findByAny(permission: String): AccessTokenPermission? =
            findAccessTokenPermissionByIdentifier(permission) ?: findAccessTokenPermissionByShortcut(permission)

        @JsonCreator
        @JvmStatic
        fun fromObject(data: Map<String, String>): AccessTokenPermission =
            findAccessTokenPermissionByIdentifier(data["identifier"]!!)!!

    }

}


