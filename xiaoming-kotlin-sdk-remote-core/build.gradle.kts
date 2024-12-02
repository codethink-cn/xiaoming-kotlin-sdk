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
    kotlin("jvm")
    id("me.him188.kotlin-jvm-blocking-bridge")
    `maven-publish`
}

dependencies {
    api(project(":xiaoming-kotlin-sdk-core"))
    api(project(":xiaoming-kotlin-sdk-remote-core-api"))

    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.log4j.slf4j2.impl)
    testRuntimeOnly(libs.log4j.core)

    api(libs.ktor.server.core.jvm)
    api(libs.ktor.server.websockets.jvm)
    api(libs.ktor.server.netty.jvm)
    api(libs.ktor.websockets)
    api(libs.ktor.client.okhttp)

    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
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