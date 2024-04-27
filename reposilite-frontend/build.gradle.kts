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

import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("com.github.node-gradle.node") version "3.2.1"
}

node {
    version.set("18.16.1")
    download.set(true)
}

//val lintTask = tasks.register<NpmTask>("lintFrontend") {
//    command.set("eslint")
//    args.set(listOf("src/**"))
//    args.set(listOf("run", "eslint"))
//    dependsOn(tasks.npmInstall)
//    inputs.dir("src")
//    inputs.dir("node_modules")
//    inputs.files("vite.config.js", "windi.config.js", "index.html", "eslint.config.js")
//    outputs.upToDateWhen { true }
//}

val buildTask = tasks.register<NpmTask>("buildFrontend") {
//    command.set("vite")
//    args.set(listOf("build"))
    args.set(listOf("run", "build"))
//    dependsOn(tasks.npmInstall, lintTask)
    inputs.dir(project.fileTree("src"))
    inputs.dir("node_modules")
    inputs.files("vite.config.js", "windi.config.js", "index.html")
    outputs.dir("${project.layout.buildDirectory.get()}/frontend")
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
