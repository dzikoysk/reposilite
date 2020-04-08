package org.panda_lang.nanomaven.util;

import org.panda_lang.utilities.commons.FileUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.IOException;

public final class YamlUtils {

    private static final Representer REPRESENTER = new Representer() {{
        getPropertyUtils().setSkipMissingProperties(true);
    }};

    private static final Yaml YAML = new Yaml(REPRESENTER);

    public static <T> T load(File file, Class<T> type) throws IOException {
        return YAML.loadAs(FileUtils.getContentOfFile(file), type);
    }

    public static void save(File file, Object value) throws IOException {
        FileUtils.overrideFile(file, YAML.dump(value));
    }

}
