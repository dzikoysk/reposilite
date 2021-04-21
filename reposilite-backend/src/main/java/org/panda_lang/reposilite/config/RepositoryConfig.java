package org.panda_lang.reposilite.config;

import java.util.regex.Pattern;

public final class RepositoryConfig {
    private static final Pattern UNESCAPED_SPACE = Pattern.compile("(?<!\\\\) ");

    private final String repositoryName;

    private RepositoryConfig(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public String toString() {
        return "RepositoryConfig[" + this.repositoryName + "]";
    }

    public String getRepositoryName() {
        return this.repositoryName;
    }

    public <T> T get(RepositoryOption<T> option) {
        return option.get(this);
    }

    public static RepositoryConfig parse(String string) {
        String[] strings = UNESCAPED_SPACE.split(string);
        String repositoryName = strings[0].startsWith(".") ? strings[0].substring(1) : strings[0];
        RepositoryConfig config = new RepositoryConfig(repositoryName);

        for (RepositoryOption<?> option : RepositoryOption.getOptions()) {
            option.parse(config, strings);
        }

        return config;
    }
}
