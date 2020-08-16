package org.panda_lang.reposilite.stats

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.reposilite.ReposiliteConstants
import org.panda_lang.utilities.commons.FileUtils

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

final class StatsStorageTest {

    @TempDir
    public File workingDirectory

    @Test
    void 'should convert old data file' () {
        def statsStorage = new StatsStorage(workingDirectory.getAbsolutePath())

        FileUtils.overrideFile(new File(workingDirectory, 'stats.yml'), 'records: {}')
        statsStorage.loadStats()

        def dataFile = new File(workingDirectory, ReposiliteConstants.STATS_FILE_NAME)
        assertTrue dataFile.exists()
        assertEquals 'records: {}', FileUtils.getContentOfFile(dataFile)
    }

}
