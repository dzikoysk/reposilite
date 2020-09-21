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

        List<FileDetailsDto> files = Arrays.asList(FileDetailsDto.of(file1), FileDetailsDto.of(file2));
        FileListDto fileListDto = new FileListDto(files);

        assertEquals(files, fileListDto.getFiles());
    }

}