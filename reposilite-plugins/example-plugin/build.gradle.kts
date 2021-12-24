group = "com.reposilite.plugins"
version = "3.0.0-alpha.14"

plugins {
    `java-library`
    application
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.panda-lang.org/releases") }
}

dependencies {
    compileOnly("org.panda-lang:reposilite:3.0.0-alpha.14")
    compileOnly("com.reposilite:journalist:1.0.10")
    implementation("org.jetbrains:annotations:23.0.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(Jar::class.java) {
    destinationDirectory.set(file("$rootDir/reposilite-backend/src/test/workspace/plugins"))
    archiveFileName.set("example-plugin.jar")
}