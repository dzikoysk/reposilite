import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "com.reposilite.plugins"

plugins {
    kotlin("jvm")
}

application {
    mainClass.set("com.reposilite.plugin.javadoc.JavadocPluginKt")
}

dependencies {
    // compileOnly("org.panda-lang:reposilite:version") for external plugins

    compileOnly(project(":reposilite-backend"))
}

tasks.withType<ShadowJar> {
    archiveFileName.set("javadoc-plugin.jar")
    destinationDirectory.set(file("$rootDir/reposilite-backend/src/test/workspace/plugins"))
    mergeServiceFiles()
}