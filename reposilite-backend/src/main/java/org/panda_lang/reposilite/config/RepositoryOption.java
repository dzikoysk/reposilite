package org.panda_lang.reposilite.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.panda_lang.reposilite.storage.FileSystemStorageProvider;
import org.panda_lang.reposilite.storage.S3StorageProvider;
import org.panda_lang.reposilite.storage.StorageProvider;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RepositoryOption<T> {
    private static final Map<String, RepositoryOption<?>> OPTIONS = new HashMap<>();

    public static final RepositoryOption<Boolean> PRIVATE = create("private", () -> false, strings -> strings[0].startsWith(".") || contains("--private", strings));
    public static final RepositoryOption<Boolean> NO_REDEPLOY = create("no-redeploy", () -> false, strings -> contains("--no-redeploy", strings)); // TODO Actually prevent redeploy
    public static final RepositoryOption<StorageProvider> STORAGE_PROVIDER = create("storage-provider", () -> null, strings -> {
        String provider = getValue("--storage-provider", strings);

        if (provider != null) {
            if (provider.equalsIgnoreCase("files")) {
                String diskQuota = getValue("--disk-quota", strings);

                if (diskQuota == null) {
                    throw new UnsupportedOperationException("'--disk-quota' cannot be null");
                } else {
                    return FileSystemStorageProvider.of(Paths.get("repositories").resolve(strings[0]), diskQuota);
                }
            } else if (provider.equalsIgnoreCase("s3")) {
                String s3BucketName = getValue("--s3-bucket", strings);
                String region = getValue("--s3-region", strings);

                if (s3BucketName == null) {
                    throw new UnsupportedOperationException("'--s3-bucket' cannot be null");
                } else if (region == null) {
                    throw new UnsupportedOperationException("'--s3-region' cannot be null");
                } else {
                    return new S3StorageProvider(s3BucketName, region);
                }
            } else if (provider.equalsIgnoreCase("rest")) {
                // TODO REST API storage endpoint
            }
        }

        throw new UnsupportedOperationException("Storage provider specifier is required");
    });

    private final String name;
    private final Map<RepositoryConfig, T> values = new HashMap<>();
    private final Function<RepositoryConfig, T> defaultValue;
    private final Function<String[], T> parser;

    private RepositoryOption(String name, Supplier<T> defaultValue, Function<String[], T> parser) {
        this.name = name;
        this.defaultValue = config -> defaultValue.get();
        this.parser = parser;
    }

    @Override
    public String toString() {
        return "RepositoryOption[" + this.name + "]";
    }

    public T get(RepositoryConfig config) {
        return this.values.computeIfAbsent(config, this.defaultValue);
    }

    public void parse(RepositoryConfig config, String[] args) {
        this.values.put(config, this.parser.apply(args));
    }

    void put(RepositoryConfig config, T value) {
        this.values.put(config, value);
    }

    public static <T> RepositoryOption<T> create(@NotNull String name, Supplier<T> defaultValue, Function<String[], T> parser) {
        RepositoryOption<T> option = new RepositoryOption<>(name, defaultValue, parser);

        OPTIONS.put(name, option);

        return option;
    }

    public static RepositoryOption<?> get(String name) {
        return OPTIONS.get(name);
    }

    public static Collection<RepositoryOption<?>> getOptions() {
        return OPTIONS.values();
    }

    private static boolean contains(String name, String[] strings) {
        // The first arg is the repository name, so we skip it.
        for (int i = 1; i < strings.length; ++i) {
            if (strings[i].equals(name)) {
                return true;
            }
        }

        return false;
    }

    private static @Nullable String getValue(String name, String[] strings) {
        String key = name + '=';
        // The first arg is the repository name, so we skip it.
        for (int i = 1; i < strings.length; ++i) {
            if (strings[i].startsWith(key)) {
                return strings[i].substring(key.length());
            }
        }

        return null;
    }
}
