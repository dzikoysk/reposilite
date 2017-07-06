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

package org.panda_lang.nanomaven.util;

import org.panda_lang.nanomaven.NanoMaven;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.InputStream;

public class ZipUtils {

    public static void unzipResource(String sourcePath, String outputDirectory) {
        InputStream is = NanoMaven.class.getResourceAsStream(sourcePath);

        if (is == null) {
            throw new RuntimeException("Resource " + sourcePath + " not found");
        }

        File output = new File(outputDirectory);

        if (!output.exists()) {
            if (!output.mkdir()) {
                throw new RuntimeException("Cannot create directory");
            }
        }

        ZipUtil.unpack(is, output);
    }

}
