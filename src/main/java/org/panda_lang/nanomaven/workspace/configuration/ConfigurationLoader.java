package org.panda_lang.nanomaven.workspace.configuration;

import org.panda_lang.nanomaven.util.FilesUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigurationLoader {

    public NanoMavenConfiguration load(String configuration) throws IOException {
        Path configurationPath = Paths.get(configuration);

        if (!FilesUtils.fileExists(configuration)) {
            FilesUtils.copyResource("/nanomaven.yml", configuration);
        }

        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(representer);

        try (InputStream input = Files.newInputStream(configurationPath)) {
            return yaml.loadAs(input, NanoMavenConfiguration.class);
        }
        catch (Exception ex) {
            throw new RuntimeException("could not load configuration", ex);
        }
    }

}
