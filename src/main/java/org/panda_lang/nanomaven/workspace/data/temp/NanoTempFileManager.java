/*
 * Copyright (c) 2017 Dzikoysk
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

package org.panda_lang.nanomaven.workspace.data.temp;

import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class NanoTempFileManager implements NanoHTTPD.TempFileManager {

    private static final File NANOHTTPD_TEMP = new File(TempDirectory.TEMP, "nanohttpd");

    public void initialize() {
        try {
            FileUtils.forceMkdir(NANOHTTPD_TEMP);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public NanoHTTPD.TempFile createTempFile(String s) throws Exception {
        NanoTempFile file = new NanoTempFile(NANOHTTPD_TEMP);
        file.prepare();

        return file;
    }

    @Override
    public void clear() {
        try {
            FileUtils.cleanDirectory(NANOHTTPD_TEMP);
        } catch (IOException e) {
            // acceptable
        }
    }

}
