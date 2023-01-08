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

import com.github.gradle.node.npm.task.NpxTask

plugins {
    id("com.github.node-gradle.node") version "3.2.1"
}

val lintTask = tasks.register<NpxTask>("lintFrontend") {
    command.set("eslint")
    args.set(listOf("src/**"))
    dependsOn(tasks.npmInstall)
    inputs.dir("src")
    inputs.dir("node_modules")
    inputs.files("vite.config.js", "windi.config.js", "index.html", ".eslintrc.js")
    outputs.upToDateWhen { true }
}

val buildTask = tasks.register<NpxTask>("buildFrontend") {
    command.set("vite")
    args.set(listOf("build"))
    dependsOn(tasks.npmInstall, lintTask)
    inputs.dir(project.fileTree("src"))
    inputs.dir("node_modules")
    inputs.files("vite.config.js", "windi.config.js", "index.html")
    outputs.dir("${project.buildDir}/frontend")
}

sourceSets {
    java {
        main {
            resources {
                srcDir(buildTask)
            }
        }
    }
}
