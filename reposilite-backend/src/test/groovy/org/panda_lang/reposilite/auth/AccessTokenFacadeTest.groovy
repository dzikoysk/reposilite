/*
 * Copyright (c) 2021 dzikoysk
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

package org.panda_lang.reposilite.auth

import groovy.transform.CompileStatic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.storage.infrastructure.FileSystemStorageProvider
import org.panda_lang.reposilite.storage.StorageProvider
import org.panda_lang.reposilite.token.AccessTokenFacade

import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class AccessTokenFacadeTest {

    @TempDir
    protected Path workingDirectory
    protected AccessTokenFacade tokenService
    protected StorageProvider storageProvider

    @BeforeEach
    void prepare() {
        this.storageProvider = FileSystemStorageProvider.of(Paths.get(""), "10GB")
        this.tokenService = new AccessTokenFacade(workingDirectory, storageProvider)
    }

    @Test
    void 'should save and load' () {
        def tempService = new AccessTokenFacade(workingDirectory, storageProvider)
        tempService.createToken('path', 'alias', 'rw')
        tempService.saveTokens()
        tokenService.loadTokens() // uses the same file

        assertEquals 'path',  tokenService.getToken('alias').get().getPath()
    }

    @Test
    void 'should create token' () {
        def result = tokenService.createToken('path', 'alias', 'rw')
        assertNotNull tokenService.getToken('alias')

        def token = result.getValue()
        assertEquals 'path', token.getPath()
        assertEquals 'alias', token.getAlias()
        assertTrue AccessTokenFacade.B_CRYPT_TOKENS_ENCODER.matches(result.getKey(), token.getSecret())

        def customResult = tokenService.createToken('custom_path', 'custom_alias', 'rw', 'secret')
        assertNotNull tokenService.getToken('custom_alias')

        def customToken = customResult.getValue()
        assertEquals 'custom_path', customToken.getPath()
        assertEquals 'custom_alias', customToken.getAlias()
        assertTrue AccessTokenFacade.B_CRYPT_TOKENS_ENCODER.matches('secret', customToken.getSecret())
    }

    @Test
    void 'should add token' () {
        def token = new Token('path', 'alias', 'secret', 'rw')
        tokenService.addToken(token)
        assertEquals token, tokenService.getToken('alias').get()
    }

    @Test
    void 'should delete token' () {
        assertTrue tokenService.deleteToken('random').isEmpty()

        tokenService.createToken('path', 'alias', 'token')
        Token token = tokenService.deleteToken('alias').get()
        assertNotNull token
        assertEquals 'alias', token.getAlias()

        assertTrue tokenService.getToken('alias').isEmpty()
    }

    @Test
    void 'should get token' () {
        assertTrue tokenService.getToken('random').isEmpty()
        tokenService.createToken('path', 'alias', 'rw')
        assertNotNull tokenService.getToken('alias')
    }

    @Test
    void 'should count tokens' () {
        assertEquals 0, tokenService.count()

        tokenService.createToken('a', 'a', 'rw')
        tokenService.createToken('b', 'b', 'rw')
        assertEquals 2, tokenService.count()

        tokenService.deleteToken('a')
        assertEquals 1, tokenService.count()
    }

    @Test
    void 'should get all tokens' () {
        assertIterableEquals Collections.emptyList(), tokenService.getTokens()

        def token = tokenService.createToken('path', 'alias', 'rw')
        assertIterableEquals Collections.singletonList(token.getValue()), tokenService.getTokens()
    }

}