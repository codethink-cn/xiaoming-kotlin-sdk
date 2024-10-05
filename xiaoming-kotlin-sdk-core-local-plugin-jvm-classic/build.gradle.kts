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
    compileOnly(project(":xiaoming-kotlin-sdk-core-local"))
    compileOnly(project(":xiaoming-kotlin-sdk-core-local-plugin-jvm"))

    val kotlinLoggingVersion: String by rootProject
    val slf4jVersion: String by rootProject
    api("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    api("org.slf4j:slf4j-api:$slf4jVersion")

    val junitVersion: String by rootProject
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.kotlinSourcesJar)
            from(components["java"])
        }
    }
}