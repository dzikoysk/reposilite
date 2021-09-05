package com.reposilite.token

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.SecureRandom
import java.util.Base64

object AccessTokenSecurityProvider {

    val B_CRYPT_TOKENS_ENCODER = BCryptPasswordEncoder()

    fun generateSecret(): String {
        val secret = ByteArray(48)
        SecureRandom().nextBytes(secret)
        return Base64.getEncoder().encodeToString(secret)
    }

}