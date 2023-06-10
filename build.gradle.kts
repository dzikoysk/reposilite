import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright (c) 2023 dzikoysk
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

    val kotlinVersion = "1.8.22"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion

    id("pl.allegro.tech.build.axion-release")
}

scmVersion {
    checks.apply {
        isUncommittedChanges = false
    }

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
        fileUpdate("reposilite-site/data/guides/developers/plugin-api.md") { version -> "\"com.reposilite:reposilite:$version\"" }
        fileUpdate("reposilite-site/data/guides/installation/docker.md") { version -> version }
        commit { version -> "Release $version" }
    }

    scmVersion {
        checks.isSnapshotDependencies = false
    }
}

allprojects {
    version = rootProject.scmVersion.version

    // Give a friendly error when building project and git tags aren't available
    // ~ https://github.com/dzikoysk/reposilite/issues/1725
    if (version == "0.1.0-SNAPSHOT") {
        throw IllegalStateException("Version is not set, please run 'git fetch --tags' command to fetch tags from main repository.")
    }

    repositories {
        maven("https://maven.reposilite.com/maven-central") {
            mavenContent {
                releasesOnly()
            }
        }
        maven("https://maven.reposilite.com/releases") {
            mavenContent {
                releasesOnly()
            }
        }
        maven("https://maven.reposilite.com/snapshots") {
            mavenContent {
                snapshotsOnly()
            }
        }
        maven("https://jitpack.io") {
            mavenContent {
                releasesOnly()
            }
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "application")
    apply(plugin = "maven-publish")

    group = "com.reposilite"

    dependencies {
        val unirest = "3.14.2"
        testImplementation("com.konghq:unirest-java:$unirest")
        testImplementation("com.konghq:unirest-objectmapper-jackson:$unirest")

        val assertJ = "3.24.2"
        testImplementation("org.assertj:assertj-core:$assertJ")

        val junit = "5.9.3"
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:$junit")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    sourceSets.main {
        java.srcDirs("src/main/kotlin")
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
            languageVersion = "1.8"
            freeCompilerArgs = listOf("-Xjvm-default=all") // For generating default methods in interfaces
        }
    }

    publishing {
        repositories {
            maven {
                name = "panda-repository"
                url = uri("https://maven.reposilite.com/${if (version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"}")
                credentials {
                    username = System.getenv("MAVEN_NAME") ?: property("mavenUser").toString()
                    password = System.getenv("MAVEN_TOKEN") ?: property("mavenPassword").toString()
                }
            }
        }

        publications {
            create<MavenPublication>("library") {
                from(components.getByName("java"))
                // Gradle generator does not support <repositories> section from Maven specification.
                // ~ https://github.com/gradle/gradle/issues/15932
                pom.withXml {
                    val repositories = asNode().appendNode("repositories")
                    project.repositories.findAll(closureOf<Any> {
                        if (this is MavenArtifactRepository && this.url.toString().startsWith("https")) {
                            val repository = repositories.appendNode("repository")
                            repository.appendNode("id", this.url.toString().replace("https://", "").replace(".", "-").replace("/", "-"))
                            repository.appendNode("url", this.url.toString())
                        }
                    })
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
