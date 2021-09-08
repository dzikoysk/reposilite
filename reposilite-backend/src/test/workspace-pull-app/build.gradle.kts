plugins {
    kotlin("jvm") version "1.5.30"
    application
}

group = "com.reposilite.test"
version = "1.0.0"

application {
    mainClass.set("ApplicationKt")
}

repositories {
    mavenCentral()
    maven {
        url = uri("http://localhost/releases")
        isAllowInsecureProtocol = true
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.reposilite.test:workspace-deploy-app:1.0.0")
}