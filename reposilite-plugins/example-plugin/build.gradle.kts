import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "com.reposilite.plugins"

application {
    mainClass.set("com.reposilite.plugin.groovy.GroovyPluginKt")
}

dependencies {
    // compileOnly("org.panda-lang:reposilite:version") for external plugins
    compileOnly(project(":reposilite-backend"))
}

tasks.withType<ShadowJar> {
    archiveFileName.set("example-plugin.jar")
    destinationDirectory.set(file("$rootDir/reposilite-backend/src/test/workspace/plugins"))
    mergeServiceFiles()
}