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

import org.apache.commons.io.FileUtils;
import org.panda_lang.nanomaven.NanoMaven;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class FilesUtils {

    public static boolean copyResource(String resourcePath, String destinationPath) {
        URL inputUrl = NanoMaven.class.getResource(resourcePath);
        File destination = new File(destinationPath);

        try {
            FileUtils.copyURLToFile(inputUrl, destination);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void createFile(String path) {
        File file = new File(path);
        file.getParentFile().mkdirs();

        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean fileExists(String file) {
        return new File(file).exists();
    }

}
