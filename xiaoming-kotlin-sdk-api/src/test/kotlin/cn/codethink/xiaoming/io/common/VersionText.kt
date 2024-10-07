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

package cn.codethink.xiaoming.io.common

import cn.codethink.xiaoming.common.toVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VersionText {
    @ParameterizedTest
    @CsvSource("1.0.0", "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0+20130313144700")
    fun testVersionParsing(version: String) = assertEquals(version, version.toVersion().toString())

    @Test
    fun testVersionComparing() {
        // Based on https://semver.org
        // 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta
        // < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0

        val versions = arrayOf(
            "1.0.0-alpha".toVersion(),
            "1.0.0-alpha.1".toVersion(),
            "1.0.0-alpha.beta".toVersion(),
            "1.0.0-beta".toVersion(),
            "1.0.0-beta.2".toVersion(),
            "1.0.0-beta.11".toVersion(),
            "1.0.0-rc.1".toVersion(),
            "1.0.0".toVersion()
        )

        for (i in 0 until versions.size - 1) {
            assertTrue(versions[i] < versions[i + 1]) {
                "Expect ${versions[i]} < ${versions[i + 1]}"
            }
        }
    }
}