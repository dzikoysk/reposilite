import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.reposilite.plugins"

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

application {
    mainClass.set("com.reposilite.plugin.groovy.GroovyPluginKt")
}

dependencies {
    compileOnly(project(":reposilite-backend"))
    implementation("org.apache.groovy:groovy:4.0.0-rc-1")
}

tasks.withType(KotlinCompile::class.java) {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("groovy-plugin.jar")
    destinationDirectory.set(file("$rootDir/reposilite-backend/src/test/workspace/plugins"))
    mergeServiceFiles()
}