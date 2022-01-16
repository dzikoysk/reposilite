import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.reposilite.plugins"

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.1"
    kotlin("jvm")
}

application {
    mainClass.set("com.reposilite.plugin.javadoc.JavadocPluginKt")
}

dependencies {
    compileOnly("net.dzikoysk:exposed-upsert:1.0.3")
    compileOnly(project(":reposilite-backend"))
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("javadoc-plugin.jar")
    destinationDirectory.set(file("$rootDir/reposilite-backend/src/test/workspace/plugins"))
    mergeServiceFiles()
}