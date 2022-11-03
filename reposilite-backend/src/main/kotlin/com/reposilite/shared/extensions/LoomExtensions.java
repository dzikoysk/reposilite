package com.reposilite.shared.extensions;

public class LoomExtensions {

    @SuppressWarnings("KotlinInternalInJava")
    public static boolean isLoomAvailable() {
        return io.javalin.util.LoomUtil.INSTANCE.getLoomAvailable();
    }

}
