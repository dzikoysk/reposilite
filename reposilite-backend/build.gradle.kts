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

//import io.gitlab.arturbosch.detekt.Detekt
//import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs

plugins {
    jacoco
    kotlin("jvm")
    kotlin("kapt")
    id("com.coditory.integration-test") version "1.4.5"
    id("com.github.johnrengelman.shadow") version "8.1.1"
//    id("io.gitlab.arturbosch.detekt").version("1.22.0")
}

application {
    mainClass.set("com.reposilite.ReposiliteLauncherKt")
}

dependencies {
    implementation(project(":reposilite-frontend"))

//    val detekt = "1.23.5"
//    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detekt")

    val kotlin = "2.0.10"
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin")

    val javalin = "6.2.0"
    api("io.javalin:javalin:$javalin")
    api("io.javalin.community.ssl:ssl-plugin:$javalin")

    val javalinOpenApi = "6.2.0"
    api("io.javalin.community.openapi:javalin-openapi-plugin:$javalinOpenApi")
    kapt("io.javalin.community.openapi:openapi-annotation-processor:$javalinOpenApi")

    val javalinRouting = "6.2.0"
    api("io.javalin.community.routing:routing-dsl:$javalinRouting")

    val bcrypt = "0.10.2"
    implementation("at.favre.lib:bcrypt:$bcrypt")

    val expressible = "1.3.6"
    api("org.panda-lang:expressible:$expressible")
    api("org.panda-lang:expressible-kt:$expressible")
    testImplementation("org.panda-lang:expressible-junit:$expressible")

    val cdn = "1.14.5"
    api("net.dzikoysk:cdn:$cdn")
    api("net.dzikoysk:cdn-kt:$cdn")

    val picocli = "4.7.6"
    kapt("info.picocli:picocli-codegen:$picocli")
    api("info.picocli:picocli:$picocli")

    val awssdk = "2.27.8"
    implementation(platform("software.amazon.awssdk:bom:$awssdk"))
    implementation("software.amazon.awssdk:s3:$awssdk")

    val awsSdkV1 = "1.12.769"
    testImplementation("com.amazonaws:aws-java-sdk-s3:$awsSdkV1")

    val exposed = "0.53.0"
    api("org.jetbrains.exposed:exposed-core:$exposed")
    api("org.jetbrains.exposed:exposed-dao:$exposed")
    api("org.jetbrains.exposed:exposed-jdbc:$exposed")
    api("org.jetbrains.exposed:exposed-java-time:$exposed")
    // Drivers
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.h2database:h2:2.3.232")

    val exposedUpsert = "1.2.2"
    api("net.dzikoysk:exposed-upsert:$exposedUpsert")

    val jackson = "2.17.2"
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jackson")

    val jsonSchema = "4.36.0"
    implementation("com.github.victools:jsonschema-generator:$jsonSchema")

    val httpClient = "1.44.2"
    implementation("com.google.http-client:google-http-client:$httpClient") { exclude(group = "commons-codec", module = "commons-codec")}
    testImplementation("com.google.http-client:google-http-client-jackson2:$httpClient")

    val commonsCoded = "1.17.1"
    api("commons-codec:commons-codec:$commonsCoded")

    val jansi = "2.4.1"
    implementation("org.fusesource.jansi:jansi:$jansi")

    val journalist = "1.0.12"
    api("com.reposilite:journalist:$journalist")
    implementation("com.reposilite:journalist-slf4j:$journalist")
    implementation("com.reposilite:journalist-tinylog:$journalist")

    val tinylog = "2.7.0"
    implementation("org.tinylog:slf4j-tinylog:$tinylog")
    implementation("org.tinylog:tinylog-api:$tinylog")
    implementation("org.tinylog:tinylog-impl:$tinylog")

    val testcontainers = "1.20.1"
    testImplementation("org.testcontainers:postgresql:$testcontainers")
    testImplementation("org.testcontainers:mariadb:$testcontainers")
    testImplementation("org.testcontainers:testcontainers:$testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainers")
    testImplementation("org.testcontainers:localstack:$testcontainers")
    testImplementation("org.testcontainers:mysql:$testcontainers")

    val ldap = "7.0.1"
    testImplementation("com.unboundid:unboundid-ldapsdk:$ldap")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("reposilite-${archiveVersion.get()}.jar")
    mergeServiceFiles()
    minimize {
        exclude(dependency("org.eclipse.jetty:.*"))
        exclude(dependency("org.eclipse.jetty.http2:.*"))
        exclude(dependency("org.eclipse.jetty.websocket:.*"))
        exclude(dependency("org.bouncycastle:.*"))
        exclude(dependency("com.fasterxml.woodstox:woodstox-core:.*"))
        exclude(dependency("commons-logging:commons-logging:.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
        exclude(dependency("org.jetbrains.exposed:.*"))
        exclude(dependency("org.xerial:sqlite-jdbc.*"))
        exclude(dependency("org.sqlite:.*"))
        exclude(dependency("mysql:.*"))
        exclude(dependency("org.mariadb.jdbc:.*"))
        exclude(dependency("org.postgresql:.*"))
        exclude(dependency("org.h2:.*"))
        exclude(dependency("com.h2database:.*"))
        exclude(dependency("org.tinylog:.*"))
        exclude(dependency("org.slf4j:.*"))
        exclude(dependency("software.amazon.awssdk:.*"))
    }
}

publishing {
    publications {
        create<MavenPublication>("bundle") {
            from(components.getByName("java"))
            artifactId = "reposilite"
            // Gradle generator does not support <repositories> section from Maven specification.
            // ~ https://github.com/gradle/gradle/issues/15932
            pom.withXml {
                val repositories = asNode().appendNode("repositories")
                project.repositories.findAll(closureOf<Any> {
                    if (this is MavenArtifactRepository && this.url.toString().startsWith("https")) {
                        val repository = repositories.appendNode("repository")
                        repository.appendNode("id", this.url.toString().replace("https://", "").replace(".", "-").replace("/", "-"))
                        repository.appendNode("url", this.url.toString())
                    }
                })
            }
        }
    }
}

tasks {
    register<Copy>("generateKotlin") {
        inputs.property("version", version)
        from("$projectDir/src/template/kotlin")
        into("$projectDir/src/generated/kotlin")
        filter(ReplaceTokens::class, "tokens" to mapOf("version" to version))
    }
    compileKotlin { dependsOn("generateKotlin") }
    sourcesJar { dependsOn("generateKotlin") }
    kotlinSourcesJar { dependsOn("generateKotlin") }
    withType<KaptGenerateStubs> { dependsOn("generateKotlin") }
}

kotlin.sourceSets.main {
    kotlin.srcDir("$projectDir/src/generated/kotlin")
}

kapt {
    arguments {
        arg("project", "${project.group}/${project.name}") // picocli requirement
    }
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.test {
    extensions.configure(JacocoTaskExtension::class) {
        setDestinationFile(file("${project.layout.buildDirectory.get()}/jacoco/jacoco.exec"))
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

    executionData(fileTree(project.layout.buildDirectory.get()).include("jacoco/*.exec"))
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
        ":reposilite-backend:test",
        ":reposilite-backend:integrationTest",
        ":reposilite-backend:jacocoTestReport",
        ":reposilite-backend:jacocoTestCoverageVerification"
    )
}

tasks["integrationTest"].mustRunAfter(tasks["test"])
tasks["jacocoTestReport"].mustRunAfter(tasks["integrationTest"])
tasks["jacocoTestCoverageVerification"].mustRunAfter(tasks["jacocoTestReport"])

//detekt {
//    buildUponDefaultConfig = true
//    allRules = false
//    config = files("$projectDir/detekt.yml")
//    autoCorrect = true
//}
//
//tasks.withType<Detekt>().configureEach {
//    jvmTarget = "11"
//}
//
//tasks.withType<DetektCreateBaselineTask>().configureEach {
//    jvmTarget = "11"
//}
