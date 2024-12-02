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

package cn.codethink.xiaoming.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionMatcherTest {
    private fun isMatched(matcher: String, version: String) = matcher.toVersionMatcher().isMatched(version.toVersion())

    @Test
    fun testParseVersionMatcher() {
        assertEquals(createGreaterThanVersionMatcher("0.1.0".toVersion()), ">0.1.0".toVersionMatcher())
        assertEquals(createGreaterThanVersionMatcher("0.1.0".toVersion()), "0.1.0<".toVersionMatcher())

        assertEquals(createGreaterThanOrEqualVersionMatcher("0.2.0".toVersion()), ">=0.2.0".toVersionMatcher())
        assertEquals(createGreaterThanOrEqualVersionMatcher("0.2.0".toVersion()), "]0.2.0".toVersionMatcher())
        assertEquals(createGreaterThanOrEqualVersionMatcher("0.2.0".toVersion()), "0.2.0=<".toVersionMatcher())
        assertEquals(createGreaterThanOrEqualVersionMatcher("0.2.0".toVersion()), "0.2.0[".toVersionMatcher())

        assertEquals(createLessThanVersionMatcher("1.1.4".toVersion()), "<1.1.4".toVersionMatcher())
        assertEquals(createLessThanVersionMatcher("1.1.4".toVersion()), "1.1.4>".toVersionMatcher())

        assertEquals(createLessThanOrEqualVersionMatcher("5.1.4".toVersion()), "<=5.1.4".toVersionMatcher())
        assertEquals(createLessThanOrEqualVersionMatcher("5.1.4".toVersion()), "[5.1.4".toVersionMatcher())
        assertEquals(createLessThanOrEqualVersionMatcher("5.1.4".toVersion()), "5.1.4]".toVersionMatcher())
        assertEquals(createLessThanOrEqualVersionMatcher("5.1.4".toVersion()), "5.1.4>=".toVersionMatcher())

        assertEquals(createIncludeVersionMatcher("0.1.0".toVersion()), "0.1.0".toVersionMatcher())
        assertEquals(createExcludeVersionMatcher("0.1.0".toVersion()), "!0.1.0".toVersionMatcher())

        assertEquals(createMajorMinorVersionPrefixMatcher(1893, 12), "1893.12.+".toVersionMatcher())
        assertEquals(createMajorVersionPrefixMatcher(26), "26.+".toVersionMatcher())

        assertEquals(
            createAndVersionMatcher(
                createGreaterThanOrEqualVersionMatcher("0.1.0".toVersion()),
                createLessThanOrEqualVersionMatcher("0.1.0".toVersion())
            ), ">=0.1.0 & <=0.1.0".toVersionMatcher()
        )

        assertEquals(
            createOrVersionMatcher(
                createAndVersionMatcher(
                    createGreaterThanOrEqualVersionMatcher("0.1.0".toVersion()),
                    createLessThanOrEqualVersionMatcher("0.1.0".toVersion())
                ),
                createGreaterThanOrEqualVersionMatcher("2.1.0".toVersion())
            ), "(>=0.1.0 & <=0.1.0) | >=2.1.0".toVersionMatcher()
        )

        assertEquals(
            createOrVersionMatcher(
                createAndVersionMatcher(
                    createGreaterThanOrEqualVersionMatcher("0.1.0".toVersion()),
                    createOrVersionMatcher(
                        createLessThanOrEqualVersionMatcher("0.1.0".toVersion()),
                        createLessThanVersionMatcher("0.0.1".toVersion())
                    )
                ),
                createGreaterThanOrEqualVersionMatcher("2.1.0".toVersion())
            ), "(>=0.1.0 & (<=0.1.0 | <0.0.1)) | >=2.1.0".toVersionMatcher()
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