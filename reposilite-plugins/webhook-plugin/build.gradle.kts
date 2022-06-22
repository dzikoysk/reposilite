import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "com.reposilite.plugins"

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm")
}

application {
    mainClass.set("com.reposilite.plugin.webhook.WebhookPluginKt")
}

dependencies {
    compileOnly(project(":reposilite-backend"))
}

tasks.withType<ShadowJar> {
    archiveFileName.set("webhook-plugin.jar")
    destinationDirectory.set(file("$rootDir/reposilite-backend/src/test/workspace/plugins"))
    mergeServiceFiles()
}