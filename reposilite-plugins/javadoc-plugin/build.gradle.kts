import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.reposilite.plugins"

plugins {
    kotlin("jvm")
}

application {
    mainClass.set("com.reposilite.plugin.javadoc.JavadocPluginKt")
}

dependencies {
    // compileOnly("org.panda-lang:reposilite:version") for external plugins

    implementation("net.dzikoysk:exposed-upsert:1.0.3")
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