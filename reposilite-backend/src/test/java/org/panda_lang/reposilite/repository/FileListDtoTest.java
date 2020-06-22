package org.panda_lang.reposilite.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileListDtoTest {

    @TempDir
    protected File temp;

    @Test
    void getFiles() throws IOException {
        File file1 = new File(temp, "file1");
        file1.createNewFile();

        File file2 = new File(temp, "file2");
        file2.createNewFile();

        List<FileDto> files = Arrays.asList(FileDto.of(file1), FileDto.of(file2));
        FileListDto fileListDto = new FileListDto(files);

        assertEquals(files, fileListDto.getFiles());
    }

}