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

package org.panda_lang.reposilite.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class YamlUtils {

    private static final Representer REPRESENTER = new Representer() {{
        this.getPropertyUtils().setSkipMissingProperties(true);
        this.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
    }};

    private static final Yaml YAML = new Yaml(REPRESENTER);

    private YamlUtils() { }

    public static <T> T load(Path file, Class<T> type) throws IOException {
        return YAML.loadAs(new ByteArrayInputStream(Files.readAllBytes(file)), type);
    }

    public static void save(Path file, Object value) throws IOException {
        Path lockedFile = file.resolveSibling(file.getFileName() + ".lock");

        if (Files.exists(lockedFile)) {
            Files.move(file, lockedFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            Files.write(lockedFile, YAML.dump(value).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } finally {
            Files.move(lockedFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
