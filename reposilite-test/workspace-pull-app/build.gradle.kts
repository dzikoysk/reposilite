plugins {
    kotlin("jvm") version "1.5.30"
    application
}

group = "com.reposilite.plugin.test"
version = "1.0.0"

application {
    mainClass.set("ApplicationKt")
}

repositories {
    mavenCentral()
    maven {
        url = uri("http://localhost/private")
        isAllowInsecureProtocol = true,
        credentials {
            username = "name"
            password = "secret"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.reposilite.plugin.test:workspace-deploy-app:1.0.0")
}