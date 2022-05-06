package org.panda_lang.reposilite.auth

import kotlinx.serialization.Serializable

/*
class TokenCollection {
    var tokens: List<Token> = emptyList()

    class Token {
        var alias: String = ""
        var path: String = ""
        var permissions: String = ""
        var token: String = ""
    }
}*/

@Serializable
data class TokenCollection(var tokens: List<Token>)

@Serializable
data class Token(
    var alias: String = "",
    var path: String = "",
    var permissions: String = "",
    var token: String = ""
)