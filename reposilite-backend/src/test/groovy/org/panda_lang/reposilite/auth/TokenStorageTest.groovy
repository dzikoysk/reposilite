package org.panda_lang.reposilite.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.ReposiliteConstants
import org.panda_lang.utilities.commons.FileUtils

import static org.junit.jupiter.api.Assertions.*

final class TokenStorageTest {

    @TempDir
    public File workingDirectory

    @Test
    void 'should convert old data file' () {
        def workspace = workingDirectory.getAbsolutePath()
        def tokenStorage = new TokenStorage(new TokenService(workspace), workspace)

        FileUtils.overrideFile(new File(workingDirectory, 'tokens.yml'), 'tokens: []')
        tokenStorage.loadTokens()

        def dataFile = new File(workingDirectory, ReposiliteConstants.TOKENS_FILE_NAME)
        assertTrue dataFile.exists()
        assertEquals 'tokens: []', FileUtils.getContentOfFile(dataFile)
    }

}
