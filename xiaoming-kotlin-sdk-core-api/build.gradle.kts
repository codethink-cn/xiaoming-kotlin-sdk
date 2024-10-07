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
    val log4jVersion: String by rootProject
    val kotlinLoggingVersion: String by rootProject
    val slf4jVersion: String by rootProject
    api("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    testImplementation("org.slf4j:slf4j-api:$slf4jVersion")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:$log4jVersion")

    val jacksonVersion: String by rootProject
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    val apacheCommonTextVersion: String by rootProject
    api("org.apache.commons:commons-text:$apacheCommonTextVersion")

    val kotlinCoroutineVersion: String by rootProject
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutineVersion")

    val apacheCommonCollectionVersion: String by rootProject
    api("org.apache.commons:commons-collections4:$apacheCommonCollectionVersion")

    val ktorVersion: String by rootProject
    api("io.ktor:ktor-server-core-jvm:$ktorVersion")
    api("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    api("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    api("io.ktor:ktor-websockets:$ktorVersion")
    api("io.ktor:ktor-client-okhttp:$ktorVersion")

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