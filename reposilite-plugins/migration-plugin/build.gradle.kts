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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "com.reposilite.plugins"

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.4.20"
}

application {
    mainClass.set("com.reposilite.plugin.swagger.SwaggerPluginKt")
}

dependencies {
    compileOnly(project(":reposilite-backend"))
    testImplementation(project(":reposilite-backend"))

    implementation("com.charleskorn.kaml:kaml:0.43.0")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("migration-plugin.jar")
    destinationDirectory.set(file("$rootDir/reposilite-backend/src/test/workspace/plugins"))
    mergeServiceFiles()
}