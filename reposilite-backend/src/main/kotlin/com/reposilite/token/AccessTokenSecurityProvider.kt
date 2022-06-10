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

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.SecureRandom
import java.util.*

object AccessTokenSecurityProvider {

    private val B_CRYPT_TOKENS_ENCODER = BCryptPasswordEncoder()

    fun generateSecret(): String {
        val secret = ByteArray(48)
        SecureRandom().nextBytes(secret)
        return Base64.getEncoder().encodeToString(secret)
    }

    fun encodeSecret(secret: String): String =
        B_CRYPT_TOKENS_ENCODER.encode(secret)

    fun matches(encryptedSecret: String, rawSecret: String): Boolean =
        B_CRYPT_TOKENS_ENCODER.matches(rawSecret, encryptedSecret)

}
