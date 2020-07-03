package org.panda_lang.reposilite.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.panda_lang.utilities.commons.FileUtils;
import org.panda_lang.utilities.commons.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDtoTest {

    @TempDir
    static File temp;
    static FileDto tempDto;
    static File file;
    static FileDto fileDto;

    @BeforeAll
    static void prepare() throws IOException {
        tempDto = FileDto.of(temp);

        file = new File(temp, "file");
        FileUtils.overrideFile(file, StringUtils.repeated(1024 * 1024, "7"));
        fileDto = FileDto.of(file);
    }

    @Test
    void compareTo() {
        assertTrue(fileDto.compareTo(tempDto) > 0);
        assertEquals(0, fileDto.compareTo(FileDto.of(file)));
    }

    @Test
    void isDirectory() {
        assertTrue(tempDto.isDirectory());
        assertFalse(fileDto.isDirectory());
    }

    @Test
    void getContentLength() {
        assertTrue(fileDto.getContentLength() != 0);
    }

    @Test
    void getDate() {
        assertTrue(fileDto.getDate().contains(Integer.toString(Calendar.getInstance().get(Calendar.YEAR))));
    }

    @Test
    void getName() {
        assertEquals("file", fileDto.getName());
    }

    @Test
    void getType() {
        assertEquals(FileDto.FILE, fileDto.getType());
        assertEquals(FileDto.DIRECTORY, tempDto.getType());
    }

}