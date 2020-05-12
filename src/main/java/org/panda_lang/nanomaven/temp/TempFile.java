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

package org.panda_lang.nanomaven.temp;

import fi.iki.elonen.NanoHTTPD;
import org.panda_lang.utilities.commons.FileUtils;
import org.panda_lang.utilities.commons.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class TempFile implements NanoHTTPD.TempFile {

    private final File file;
    private OutputStream outputStream;

    public TempFile(File tempDirectory) {
        this.file = new File(tempDirectory, UUID.randomUUID().toString());
    }

    public void prepare() {
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete() {
        IOUtils.close(outputStream);
        FileUtils.delete(file);
    }

    @Override
    public String getName() {
        return file.getAbsolutePath();
    }

    @Override
    public OutputStream open() throws Exception {
        this.outputStream = new FileOutputStream(file);
        return outputStream;
    }

}
