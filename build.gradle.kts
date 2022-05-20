import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright (c) 2022 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    `java-library`
    application
    `maven-publish`

    val kotlinVersion = "1.6.20"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion

    id("pl.allegro.tech.build.axion-release")
}

scmVersion {
    tag.apply {
        prefix = ""
    }
    nextVersion.apply {
        suffix = "next"
    }
    versionIncrementer("incrementPrerelease")
    hooks.apply {
        fileUpdate(".github/README.md") { version -> "reposilite-$version.jar" }
        fileUpdate(".github/README.md") { version -> "dzikoysk/reposilite:$version" }
        fileUpdate("docker-compose.yml") { version -> "image: reposilite:$version" }
        fileUpdate("reposilite-backend/src/main/kotlin/com/reposilite/Reposilite.kt") { version -> "const val VERSION = \"$version\"" }
        fileUpdate("reposilite-frontend/package.json") { version -> "\"version\": \"$version\"" }
        fileUpdate("reposilite-frontend/package-lock.json") { version -> "\"version\": \"$version\"" }
        fileUpdate("reposilite-site/data/guides/developers/endpoints.md") { version -> "\"version\": \"$version\"" }
        fileUpdate("reposilite-site/data/guides/developers/plugin-api.md") { version -> "\"org.panda-lang:reposilite:$version\"" }
        fileUpdate("reposilite-site/data/guides/installation/docker.md") { version -> version }
        fileUpdate("reposilite-site/data/plugins/javadoc.md") { version -> "reposilite-$version" }
        fileUpdate("reposilite-site/data/plugins/javadoc.md") { version -> "reposilite/$version" }
        commit { version -> "Release $version" }
    }
}

allprojects {
    version = rootProject.scmVersion.version

    repositories {
        mavenCentral()
        // maven {
        //     url = uri("http://localhost/releases")
        //     isAllowInsecureProtocol = true
        // }
        maven { url = uri("https://maven.reposilite.com/releases") }
        maven { url = uri("https://jitpack.io") }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "application")
    apply(plugin = "maven-publish")

    dependencies {
        val junit = "5.8.2"
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:$junit")
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    sourceSets.main {
        java.srcDirs("src/main/kotlin")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
            languageVersion = "1.6"
            freeCompilerArgs = listOf("-Xjvm-default=all") // For generating default methods in interfaces
        }
    }

    publishing {
        repositories {
            maven {
                name = "panda-repository"
                url = uri("https://maven.reposilite.com/${if (version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"}")
                credentials {
                    username = System.getenv("MAVEN_NAME") ?: ""
                    password = System.getenv("MAVEN_TOKEN") ?: ""
                }
            }
        }
    }

    tasks.withType<Test> {
        testLogging {
            events(
                TestLogEvent.STARTED,
                TestLogEvent.PASSED,
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED
            )
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
        }

        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2)
            .takeIf { it > 0 }
            ?: 1

        useJUnitPlatform()
    }
}
