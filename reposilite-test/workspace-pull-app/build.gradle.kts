/*
 * Copyright (c) 2020-2026 dzikoysk
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