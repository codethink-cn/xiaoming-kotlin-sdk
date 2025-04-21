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
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.blocking.bridge)
    alias(libs.plugins.build.config)
    `maven-publish`
}

fun Any.toDoubleQuotedString() = "\"$this\""

buildConfig {
    buildConfigField("String", "SDK_VERSION", project.version.toDoubleQuotedString())
    buildConfigField("String", "SDK_GROUP", project.group.toDoubleQuotedString())
}

dependencies {
    api(libs.kotlin.logging)
    api(libs.blocking.bridge)

    api(libs.jackson.databind)
    api(libs.jackson.annotations)
    api(libs.jackson.module.kotlin)

    api(libs.apache.commons.text)
    api(libs.apache.commons.collection)

    api(libs.kotlin.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("xiaoming/sdk.properties") {
        expand(
            mapOf(
                "group" to project.group,
                "name" to project.name,
                "version" to project.version,
                "standard" to 1893,
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