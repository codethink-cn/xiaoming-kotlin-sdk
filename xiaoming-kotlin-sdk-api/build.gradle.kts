/*
 * Copyright 2024 CodeThink Technologies and contributors.
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
    kotlin("jvm") version "2.0.20"
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    val kotlinLoggingVersion: String by rootProject
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")

    val jacksonVersion: String by rootProject
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    val apacheCommonTextVersion: String by rootProject
    implementation("org.apache.commons:commons-text:$apacheCommonTextVersion")

    val kotlinCoroutineVersion: String by rootProject
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutineVersion")

    val apacheCommonCollectionVersion: String by rootProject
    implementation("org.apache.commons:commons-collections4:$apacheCommonCollectionVersion")

    val ktorVersion: String by rootProject
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")

    val junitVersion: String by rootProject
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("xiaoming/sdk.properties") {
        val protocol: String by rootProject
        expand(
            mapOf(
                "group" to project.group,
                "name" to project.name,
                "version" to project.version,
                "protocol" to protocol,
            )
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.kotlinSourcesJar)
            from(components["java"])
        }
    }
}