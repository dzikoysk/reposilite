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
