/*
 * Copyright (c) 2020 Dzikoysk
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

package org.panda_lang.reposilite.auth;

import org.panda_lang.utilities.commons.collection.Pair;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final BCryptPasswordEncoder B_CRYPT_TOKENS_ENCODER = new BCryptPasswordEncoder();

    private final Map<String, Token> tokens = new HashMap<>();
    private final TokenStorage database;

    public TokenService(String workingDirectory) {
        this.database = new TokenStorage(this, workingDirectory);
    }

    public void load() throws IOException {
        this.database.loadTokens();
    }

    public void save() throws IOException {
        this.database.saveTokens();
    }

    public Pair<String, Token> createToken(String path, String alias) {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        return createToken(path, alias, Base64.getEncoder().encodeToString(randomBytes));
    }

    public Pair<String, Token> createToken(String path, String alias, String token) {
        String encodedToken = B_CRYPT_TOKENS_ENCODER.encode(token);
        return new Pair<>(token, addToken(new Token(path, alias, encodedToken)));
    }

    Token addToken(Token token) {
        this.tokens.put(token.getAlias(), token);
        return token;
    }

    Token deleteToken(String alias) {
        return tokens.remove(alias);
    }

    Token getToken(String alias) {
        return tokens.get(alias);
    }

    public int count() {
        return tokens.size();
    }

    Collection<Token> getTokens() {
        return tokens.values();
    }

}
