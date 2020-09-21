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

class FileDetailsDtoTest {

    @TempDir
    static File temp;
    static FileDetailsDto tempDto;
    static File file;
    static FileDetailsDto fileDetails;

    @BeforeAll
    static void prepare() throws IOException {
        tempDto = FileDetailsDto.of(temp);

        file = new File(temp, "file");
        FileUtils.overrideFile(file, StringUtils.repeated(1024 * 1024, "7"));
        fileDetails = FileDetailsDto.of(file);
    }

    @Test
    void compareTo() {
        assertTrue(fileDetails.compareTo(tempDto) > 0);
        assertEquals(0, fileDetails.compareTo(FileDetailsDto.of(file)));
    }

    @Test
    void isDirectory() {
        assertTrue(tempDto.isDirectory());
        assertFalse(fileDetails.isDirectory());
    }

    @Test
    void getContentLength() {
        assertTrue(fileDetails.getContentLength() != 0);
    }

    @Test
    void getDate() {
        assertTrue(fileDetails.getDate().contains(Integer.toString(Calendar.getInstance().get(Calendar.YEAR))));
    }

    @Test
    void getName() {
        assertEquals("file", fileDetails.getName());
    }

    @Test
    void getType() {
        assertEquals(FileDetailsDto.FILE, fileDetails.getType());
        assertEquals(FileDetailsDto.DIRECTORY, tempDto.getType());
    }

}