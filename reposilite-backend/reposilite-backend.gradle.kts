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

import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED

plugins {
    kotlin("jvm") version "1.5.20"
    kotlin("kapt") version "1.5.20"
    application
    jacoco
    `maven-publish`
}

group = "org.panda-lang"
version = "3.0.0-SNAPSHOT"

application {
    mainClass.set("com.reposilite.ReposiliteLauncher")
}

publishing {
    repositories {
        maven {
            credentials {
                username = property("mavenUser") as String
                password = property("mavenPassword") as String
            }
            name = "panda-repository"
            url = uri("https://repo.panda-lang.org/releases")
        }
    }
    publications {
        create<MavenPublication>("library") {
            groupId = "org.panda-lang"
            artifactId = "reposilite"
            from(components.getByName("java"))
        }
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.panda-lang.org/releases") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    val expressible = "1.0.5"
    implementation("org.panda-lang:expressible:$expressible")
    implementation("org.panda-lang:expressible-kt:$expressible")

    val awssdk = "2.15.15"
    implementation("software.amazon.awssdk:bom:$awssdk")
    implementation("software.amazon.awssdk:s3:$awssdk")

    val exposed = "0.32.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposed")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed")
    implementation("net.dzikoysk:exposed-upsert:1.0.0")
    implementation("com.h2database:h2:1.4.199")

    val fuel = "2.3.1"
    implementation("com.github.kittinunf.fuel:fuel:$fuel")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:$fuel")

    val openapi = "1.1.0"
    kapt("io.javalin-rfc:openapi-annotation-processor:$openapi")
    implementation("io.javalin-rfc:javalin-openapi-plugin:$openapi")
    implementation("io.javalin-rfc:javalin-swagger-plugin:$openapi")

    val javalinRfcs = "1.0.1"
    implementation("com.reposilite.javalin-rfcs:javalin-context:$javalinRfcs")
    implementation("com.reposilite.javalin-rfcs:javalin-coroutines:$javalinRfcs")
    implementation("com.reposilite.javalin-rfcs:javalin-error:$javalinRfcs")
    implementation("com.reposilite.javalin-rfcs:javalin-mimetypes:$javalinRfcs")
    implementation("com.reposilite.javalin-rfcs:javalin-openapi:$javalinRfcs")
    implementation("com.reposilite.javalin-rfcs:javalin-routing:$javalinRfcs")

    val javalin = "4.0.0.RC0"
    implementation("io.javalin:javalin:$javalin")

    val picocli = "4.6.1"
    kapt("info.picocli:picocli-codegen:$picocli")
    implementation("info.picocli:picocli:$picocli")

    val dynamicLogger = "1.0.2"
    implementation("net.dzikoysk:dynamic-logger:$dynamicLogger")
    implementation("net.dzikoysk:dynamic-logger-slf4j:$dynamicLogger")

    val tinylog = "2.3.1"
    implementation("org.tinylog:slf4j-tinylog:$tinylog")
    implementation("org.tinylog:tinylog-api:$tinylog")
    implementation("org.tinylog:tinylog-impl:$tinylog")

    implementation("net.dzikoysk:cdn:1.9.1")
    implementation("com.google.http-client:google-http-client:1.39.2")
    implementation("org.springframework.security:spring-security-crypto:5.4.6")
    implementation("commons-io:commons-io:2.8.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.12.3")
    implementation("org.fusesource.jansi:jansi:2.3.2")

    /* Tests */

    testImplementation("com.google.http-client:google-http-client-jackson2:1.39.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kapt {
    arguments {
        arg("project", "${project.group}/${project.name}")
    }
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Test> {
    testLogging {
        events(STARTED, PASSED, FAILED, SKIPPED)
        exceptionFormat = FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    useJUnitPlatform()
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

        csv.required.set(true)
        xml.outputLocation.set(file("$buildDir/reports/jacoco/report.xml"))
    }

    finalizedBy("jacocoTestCoverageVerification")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.10".toBigDecimal()
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

    dependsOn(":test",
        ":jacocoTestReport",
        ":jacocoTestCoverageVerification")

    tasks["jacocoTestReport"].mustRunAfter(tasks["test"])
    tasks["jacocoTestCoverageVerification"].mustRunAfter(tasks["jacocoTestReport"])
}

/*
tasks.register("depsize") {
    description = "Prints dependencies for \"default\" configuration"
    doLast { listConfigurationDependencies(configurations["default"])  }
}

tasks.register("depsize-all-configurations") {
    description = "Prints dependencies for all available configurations"
    doLast {
        configurations
            .filter { it.isCanBeResolved }
            .forEach { listConfigurationDependencies(it) }
    }
}

fun listConfigurationDependencies(configuration: Configuration ) {
    val formatStr = "%,10.2f"
    val size = configuration.sumOf { it.length() / (1024.0 * 1024.0) }
    val out = StringBuffer()
    out.append("\nConfiguration name: \"${configuration.name}\"\n")

    if (size > 0) {
        out.append("Total dependencies size:".padEnd(65))
        out.append("${String.format(formatStr, size)} Mb\n\n")

        configuration
            .sortedBy { -it.length() }
            .forEach {
                out.append(it.name.padEnd(65))
                out.append("${String.format(formatStr, (it.length() / 1024.0))} kb\n")
            }
    }
    else {
        out.append("No dependencies found")
    }

    println(out)
}
 */