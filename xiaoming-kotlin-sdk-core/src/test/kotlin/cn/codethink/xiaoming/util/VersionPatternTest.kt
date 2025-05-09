/*
 * Copyright 2025 CodeThink Technologies and contributors.
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

package cn.codethink.xiaoming.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionPatternTest {
    private fun isMatched(matcher: String, version: String) = matcher.toVersionPattern().matches(version.toVersion())

    @Test
    fun testParseVersionPattern() {
        assertEquals(GreaterThanVersionPattern("0.1.0".toVersion()), ">0.1.0".toVersionPattern())
        assertEquals(GreaterThanVersionPattern("0.1.0".toVersion()), "0.1.0<".toVersionPattern())

        assertEquals(GreaterThanOrEqualVersionPattern("0.2.0".toVersion()), ">=0.2.0".toVersionPattern())
        assertEquals(GreaterThanOrEqualVersionPattern("0.2.0".toVersion()), "]0.2.0".toVersionPattern())
        assertEquals(GreaterThanOrEqualVersionPattern("0.2.0".toVersion()), "0.2.0=<".toVersionPattern())
        assertEquals(GreaterThanOrEqualVersionPattern("0.2.0".toVersion()), "0.2.0[".toVersionPattern())

        assertEquals(LessThanVersionPattern("1.1.4".toVersion()), "<1.1.4".toVersionPattern())
        assertEquals(LessThanVersionPattern("1.1.4".toVersion()), "1.1.4>".toVersionPattern())

        assertEquals(LessThanOrEqualVersionPattern("5.1.4".toVersion()), "<=5.1.4".toVersionPattern())
        assertEquals(LessThanOrEqualVersionPattern("5.1.4".toVersion()), "[5.1.4".toVersionPattern())
        assertEquals(LessThanOrEqualVersionPattern("5.1.4".toVersion()), "5.1.4]".toVersionPattern())
        assertEquals(LessThanOrEqualVersionPattern("5.1.4".toVersion()), "5.1.4>=".toVersionPattern())

        assertEquals(IncludeVersionPattern("0.1.0".toVersion()), "0.1.0".toVersionPattern())
        assertEquals(ExcludeVersionPattern("0.1.0".toVersion()), "!0.1.0".toVersionPattern())

        assertEquals(MajorMinorVersionPrefixPattern(1893, 12), "1893.12.+".toVersionPattern())
        assertEquals(MajorVersionPrefixPattern(26), "26.+".toVersionPattern())

        assertEquals(
            AndVersionPattern(
                GreaterThanOrEqualVersionPattern("0.1.0".toVersion()),
                LessThanOrEqualVersionPattern("0.1.0".toVersion())
            ), ">=0.1.0 & <=0.1.0".toVersionPattern()
        )

        assertEquals(
            OrVersionPattern(
                AndVersionPattern(
                    GreaterThanOrEqualVersionPattern("0.1.0".toVersion()),
                    LessThanOrEqualVersionPattern("0.1.0".toVersion())
                ),
                GreaterThanOrEqualVersionPattern("2.1.0".toVersion())
            ), "(>=0.1.0 & <=0.1.0) | >=2.1.0".toVersionPattern()
        )

        assertEquals(
            OrVersionPattern(
                AndVersionPattern(
                    GreaterThanOrEqualVersionPattern("0.1.0".toVersion()),
                    OrVersionPattern(
                        LessThanOrEqualVersionPattern("0.1.0".toVersion()),
                        LessThanVersionPattern("0.0.1".toVersion())
                    )
                ),
                GreaterThanOrEqualVersionPattern("2.1.0".toVersion())
            ), "(>=0.1.0 & (<=0.1.0 | <0.0.1)) | >=2.1.0".toVersionPattern()
        )
    }

    @Test
    fun testExtractMatched() {
        assertTrue(isMatched("0.1.0", "0.1.0"))
        assertFalse(isMatched("0.2.0", "2.0.0"))

        assertTrue(isMatched(">=0.1.0", "0.1.0"))
        assertTrue(isMatched(">=0.1.0", "0.1.1"))
        assertFalse(isMatched(">=0.1.0", "0.0.9"))

        assertTrue(isMatched(">0.1.0", "0.1.1"))
        assertFalse(isMatched(">0.1.0", "0.1.0"))
        assertFalse(isMatched(">0.1.0", "0.0.9"))

        assertTrue(isMatched("<=0.1.0", "0.1.0"))
        assertTrue(isMatched("<=0.1.0", "0.0.9"))
        assertFalse(isMatched("<=0.1.0", "0.1.1"))

        assertTrue(isMatched("<0.1.0", "0.0.9"))
        assertFalse(isMatched("<0.1.0", "0.1.0"))
        assertFalse(isMatched("<0.1.0", "0.1.1"))
    }
}