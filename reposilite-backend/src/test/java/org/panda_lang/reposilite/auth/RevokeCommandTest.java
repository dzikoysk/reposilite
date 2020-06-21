package org.panda_lang.reposilite.auth;

import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevokeCommandTest extends ReposiliteIntegrationTest {

    @Test
    void shouldRevokeExistingToken() {
        TokenService tokenService = reposilite.getTokenService();
        tokenService.addToken(new Token("path", "alias", "secret"));

        assertTrue(new RevokeCommand("alias").execute(reposilite));
        assertNull(tokenService.getToken("alias"));
    }

    @Test
    void shouldFalseIfTokenDoesNotExist() {
        assertFalse(new RevokeCommand("unknown_token").execute(super.reposilite));
    }

    @Test
    void shouldFalseIfFileIsNotAvailable() throws Exception {
        super.reposilite.getTokenService().addToken(new Token("path", "alias", "secret"));

        File tokensFile = new File(super.workingDirectory, "tokens.yml");
        RandomAccessFile randomAccessFile = new RandomAccessFile(tokensFile, "rw");
        FileChannel channel = randomAccessFile.getChannel();
        FileLock lock = channel.lock();

        assertFalse(new RevokeCommand("alias").execute(reposilite));
        lock.release();
        channel.close();
        assertTrue(new RevokeCommand("alias").execute(reposilite));
    }

}