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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class FileListDtoTest {

    @TempDir
    protected Path temp

    @Test
    void 'should return list of files' () {
        def file1 = temp.resolve("file1")
        Files.createFile(file1)

        def file2 = temp.resolve("file2")
        Files.createFile(file2)

        def files = Arrays.asList(FileDetailsDto.of(file1), FileDetailsDto.of(file2))
        def fileListDto = new FileListDto(files)

        assertEquals files, fileListDto.getFiles()
    }

}