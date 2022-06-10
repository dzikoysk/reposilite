plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://plugins.gradle.org/m2")
}

dependencies {
    implementation("pl.allegro.tech.build:axion-release-plugin:1.13.6")
}