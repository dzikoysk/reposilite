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

package org.panda_lang.reposilite.repository

import groovy.transform.CompileStatic
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panda_lang.utilities.commons.StringUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class FileDetailsDtoTest {

    @TempDir
    protected static Path temp

    static FileDetailsDto tempDto
    static Path path
    static FileDetailsDto fileDetails

    @BeforeAll
    static void prepare() throws IOException {
        tempDto = FileDetailsDto.of(temp)

        path = temp.resolve("file")
        Files.write(path, StringUtils.repeated(1024 * 1024, "7").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        fileDetails = FileDetailsDto.of(path)
    }

    @Test
    void compareTo() {
        assertTrue(fileDetails.compareTo(tempDto) > 0)
        assertEquals(0, fileDetails.compareTo(FileDetailsDto.of(path)))
    }

    @Test
    void isDirectory() {
        assertTrue(tempDto.isDirectory())
        assertFalse(fileDetails.isDirectory())
    }

    @Test
    void getContentLength() {
        assertTrue(fileDetails.getContentLength() != 0)
    }

    @Test
    void getDate() {
        assertTrue(fileDetails.getDate().contains(Integer.toString(Calendar.getInstance().get(Calendar.YEAR))))
    }

    @Test
    void getName() {
        assertEquals("file", fileDetails.getName())
    }

    @Test
    void getType() {
        assertEquals(FileDetailsDto.FILE, fileDetails.getType())
        assertEquals(FileDetailsDto.DIRECTORY, tempDto.getType())
    }

}