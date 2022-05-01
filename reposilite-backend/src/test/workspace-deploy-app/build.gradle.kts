import org.jetbrains.kotlin.gradle.plugin.statistics.ReportStatisticsToElasticSearch.password
import org.jetbrains.kotlin.gradle.plugin.statistics.ReportStatisticsToElasticSearch.url

plugins {
    kotlin("jvm") version "1.5.30"
    application
    `maven-publish`
}

group = "com.reposilite.plugin.test"
version = "1.0.0-SNAPSHOT"

application {
    mainClass.set("ApplicationKt")
}

dependencies {
    implementation(kotlin("stdlib"))
}

repositories {
    mavenCentral()
}

publishing {
    repositories {
        maven {
            credentials {
                username = "name"
                password = "secret"
            }
            name = "local-repository"
            url = uri("http://localhost/snapshots")
            isAllowInsecureProtocol = true
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components.getByName("java"))
        }
    }
}