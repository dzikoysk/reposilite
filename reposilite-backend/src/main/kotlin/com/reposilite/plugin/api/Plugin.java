package com.reposilite.plugin.api;

import com.reposilite.ReposiliteKt;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents plugin metadata, should be used to annotate {@link com.reposilite.plugin.api.ReposilitePlugin}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Plugin {

    /**
     * @return the name of this plugin
     */
    String name();

    /**
     * @return version of plugin, by default it is same as the current Reposilite version
     * @see com.reposilite.ReposiliteKt#VERSION
     */
    String version() default ReposiliteKt.VERSION;

    /**
     * @return array of plugins required to launch before this one
     */
    String[] dependencies() default {};

}
