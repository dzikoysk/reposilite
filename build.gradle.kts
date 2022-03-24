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

    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
}

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.panda-lang.org/releases") }
        maven { url = uri("https://jitpack.io") }
        // maven {
        //     url = uri("http://localhost/releases")
        //     isAllowInsecureProtocol = true
        // }
    }
}

subprojects {
    version = "3.0.0-alpha.23"

    apply(plugin = "java-library")
    apply(plugin = "application")
    apply(plugin = "maven-publish")

    dependencies {

    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        repositories {
            maven {
                name = "panda-repository"
                url = uri("https://repo.panda-lang.org/${if (version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"}")
                credentials {
                    username = System.getenv("MAVEN_NAME") ?: ""
                    password = System.getenv("MAVEN_TOKEN") ?: ""
                }
            }
        }
    }
}