/*
 * Copyright (c) 2021 dzikoysk
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
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.panda-lang"
version = "3.0.0-alpha.8"

plugins {
    `java-library`
    `maven-publish`
    application
    jacoco
    idea

    val kotlinVersion = "1.6.0-RC2"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion

    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("com.coditory.integration-test") version "1.3.0"
}

application {
    mainClass.set("com.reposilite.ReposiliteLauncherKt")
}

dependencies {
    val kotlin = "1.6.0-RC2"
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin")

    val expressible = "1.1.1"
    implementation("org.panda-lang:expressible:$expressible")
    implementation("org.panda-lang:expressible-kt:$expressible")
    implementation("org.panda-lang:expressible-kt-coroutines:$expressible")
    testImplementation("org.panda-lang:expressible-junit:$expressible")

    val cdn = "1.11.6"
    implementation("net.dzikoysk:cdn:$cdn")
    implementation("net.dzikoysk:cdn-kt:$cdn")

    val awssdk = "2.17.72"
    implementation(platform("software.amazon.awssdk:bom:$awssdk"))
    implementation("software.amazon.awssdk:s3:$awssdk")
    testImplementation("com.amazonaws:aws-java-sdk-s3:1.12.100")

    val exposed = "0.36.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposed")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed")

    implementation("net.dzikoysk:exposed-upsert:1.0.3")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("mysql:mysql-connector-java:8.0.27")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.4")

    val springSecurityCrypto = "5.5.3"
    implementation("org.springframework.security:spring-security-crypto:$springSecurityCrypto")

    val openapi = "1.1.1"
    kapt("io.javalin-rfc:openapi-annotation-processor:$openapi")
    implementation("io.javalin-rfc:javalin-openapi-plugin:$openapi")
    implementation("io.javalin-rfc:javalin-swagger-plugin:$openapi")

    val javalinRfcs = "4.1.0"
    implementation("com.reposilite.javalin-rfcs:javalin-context:$javalinRfcs")
    implementation("com.reposilite.javalin-rfcs:javalin-routing:$javalinRfcs")

    //implementation("io.javalin:javalin:4.1.1")
    implementation("com.github.dzikoysk.javalin:javalin:760159a7c4")
    @Suppress("GradlePackageUpdate")
    implementation("org.eclipse.jetty:jetty-server:9.4.44.v20210927")

    val picocli = "4.6.1"
    kapt("info.picocli:picocli-codegen:$picocli")
    implementation("info.picocli:picocli:$picocli")

    val jackson = "2.13.0"
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jackson")

    val httpClient = "1.40.1"
    implementation("com.google.http-client:google-http-client:$httpClient")
    testImplementation("com.google.http-client:google-http-client-jackson2:$httpClient")

    val commonsIO = "20030203.000550"
    implementation("commons-io:commons-io:$commonsIO")

    val jline = "3.21.0"
    implementation("org.jline:jline:$jline")

    val jansi = "2.4.0"
    implementation("org.fusesource.jansi:jansi:$jansi")

    val journalist = "1.0.10"
    implementation("com.reposilite:journalist:$journalist")
    implementation("com.reposilite:journalist-slf4j:$journalist")
    implementation("com.reposilite:journalist-tinylog:$journalist")

    val tinylog = "2.3.2"
    implementation("org.tinylog:slf4j-tinylog:$tinylog")
    implementation("org.tinylog:tinylog-api:$tinylog")
    implementation("org.tinylog:tinylog-impl:$tinylog")

    val unirest = "3.13.3"
    testImplementation("com.konghq:unirest-java:$unirest")
    testImplementation("com.konghq:unirest-objectmapper-jackson:$unirest")

    val testcontainers = "1.16.2"
    testImplementation("org.testcontainers:mariadb:$testcontainers")
    testImplementation("org.testcontainers:testcontainers:$testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainers")
    testImplementation("org.testcontainers:localstack:$testcontainers")

    val junit = "5.8.1"
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
}

repositories {
    // maven {
    //     url = uri("http://localhost/releases")
    //     isAllowInsecureProtocol = true
    // }
    // mavenCentral()
    maven { url = uri("https://repo.panda-lang.org/releases") }
    maven { url = uri("https://jitpack.io") }
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withJavadocJar()
    withSourcesJar()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        languageVersion = "1.6"
    }
}

publishing {
    repositories {
        maven {
            credentials {
                username = property("mavenUser") as String
                password = property("mavenPassword") as String
            }
            name = "panda-repository"
            url = uri("https://repo.panda-lang.org/${if (version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"}")
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components.getByName("java"))
            artifactId = "reposilite"
            // Gradle generator does not support <repositories> section from Maven specification.
            // ~ https://github.com/gradle/gradle/issues/15932
            pom.withXml {
                val repositories = asNode().appendNode("repositories")
                project.repositories.findAll(closureOf<Any> {
                    if (this is MavenArtifactRepository && this.url.toString().startsWith("https")) {
                        val repository = repositories.appendNode("repository")
                        repository.appendNode("id", this.name)
                        repository.appendNode("url", this.url.toString())
                    }
                })
            }
        }
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("reposilite-${archiveVersion.get()}.jar")
    mergeServiceFiles()
    minimize {
        exclude(dependency("org.eclipse.jetty:.*"))
        exclude(dependency("org.eclipse.jetty.websocket:.*"))
        exclude(dependency("com.fasterxml.woodstox:woodstox-core:.*"))
        exclude(dependency("commons-logging:commons-logging:.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
        exclude(dependency("org.jetbrains.exposed:.*"))
        exclude(dependency("org.xerial:sqlite-jdbc.*"))
        exclude(dependency("org.sqlite:.*"))
        exclude(dependency("mysql:.*"))
        exclude(dependency("org.h2:.*"))
        exclude(dependency("com.h2database:.*"))
        exclude(dependency("org.tinylog:.*"))
        exclude(dependency("org.slf4j:.*"))
        exclude(dependency("software.amazon.awssdk:.*"))
    }
}

kapt {
    arguments {
        arg("project", "${project.group}/${project.name}") // picocli requirement
    }
}

tasks.withType<Test> {
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }

    useJUnitPlatform()
}

tasks.withType<Test> {
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.test {
    extensions.configure(JacocoTaskExtension::class) {
        setDestinationFile(file("$buildDir/jacoco/jacoco.exec"))
    }

    finalizedBy("jacocoTestReport")
}

tasks.jacocoTestReport {
    reports {
        html.required.set(false)
        csv.required.set(false)
        xml.required.set(true)
        xml.outputLocation.set(file("./build/reports/jacoco/reposilite-backend-report.xml"))
    }

    executionData(fileTree(project.buildDir).include("jacoco/*.exec"))
    finalizedBy("jacocoTestCoverageVerification")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.0".toBigDecimal()
            }
        }
        rule {
            enabled = true
            element = "CLASS"
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.0".toBigDecimal()
            }
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.0".toBigDecimal()
            }
            excludes = listOf()
        }
    }
}

val testCoverage by tasks.registering {
    group = "verification"
    description = "Runs the unit tests with coverage"

    dependsOn(
        ":test",
        ":jacocoTestReport",
        ":jacocoTestCoverageVerification"
    )

    tasks["jacocoTestReport"].mustRunAfter(tasks["test"])
    tasks["jacocoTestCoverageVerification"].mustRunAfter(tasks["jacocoTestReport"])
}