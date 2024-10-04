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

import cn.codethink.xiaoming.common.CompositeVersionMatcher
import cn.codethink.xiaoming.common.ExcludeVersionMatcher
import cn.codethink.xiaoming.common.GreaterThanOrEqualVersionMatcher
import cn.codethink.xiaoming.common.GreaterThanVersionMatcher
import cn.codethink.xiaoming.common.IncludeVersionMatcher
import cn.codethink.xiaoming.common.LessThanOrEqualVersionMatcher
import cn.codethink.xiaoming.common.LessThanVersionMatcher
import cn.codethink.xiaoming.common.MajorMinorVersionPrefixMatcher
import cn.codethink.xiaoming.common.MajorVersionPrefixMatcher
import cn.codethink.xiaoming.common.toVersion
import cn.codethink.xiaoming.common.toVersionMatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionMatcherTest {
    private fun isMatched(matcher: String, version: String) = matcher.toVersionMatcher().isMatched(version.toVersion())

    @Test
    fun testParseVersionMatcher() {
        assertEquals(GreaterThanVersionMatcher("0.1.0".toVersion()), ">0.1.0".toVersionMatcher())
        assertEquals(GreaterThanVersionMatcher("0.1.0".toVersion()), ")0.1.0".toVersionMatcher())
        assertEquals(GreaterThanVersionMatcher("0.1.0".toVersion()), "0.1.0<".toVersionMatcher())
        assertEquals(GreaterThanVersionMatcher("0.1.0".toVersion()), "0.1.0(".toVersionMatcher())

        assertEquals(GreaterThanOrEqualVersionMatcher("0.2.0".toVersion()), ">=0.2.0".toVersionMatcher())
        assertEquals(GreaterThanOrEqualVersionMatcher("0.2.0".toVersion()), "]0.2.0".toVersionMatcher())
        assertEquals(GreaterThanOrEqualVersionMatcher("0.2.0".toVersion()), "0.2.0=<".toVersionMatcher())
        assertEquals(GreaterThanOrEqualVersionMatcher("0.2.0".toVersion()), "0.2.0[".toVersionMatcher())

        assertEquals(LessThanVersionMatcher("1.1.4".toVersion()), "<1.1.4".toVersionMatcher())
        assertEquals(LessThanVersionMatcher("1.1.4".toVersion()), "(1.1.4".toVersionMatcher())
        assertEquals(LessThanVersionMatcher("1.1.4".toVersion()), "1.1.4)".toVersionMatcher())
        assertEquals(LessThanVersionMatcher("1.1.4".toVersion()), "1.1.4>".toVersionMatcher())

        assertEquals(LessThanOrEqualVersionMatcher("5.1.4".toVersion()), "<=5.1.4".toVersionMatcher())
        assertEquals(LessThanOrEqualVersionMatcher("5.1.4".toVersion()), "[5.1.4".toVersionMatcher())
        assertEquals(LessThanOrEqualVersionMatcher("5.1.4".toVersion()), "5.1.4]".toVersionMatcher())
        assertEquals(LessThanOrEqualVersionMatcher("5.1.4".toVersion()), "5.1.4>=".toVersionMatcher())

        assertEquals(IncludeVersionMatcher("0.1.0".toVersion()), "0.1.0".toVersionMatcher())
        assertEquals(ExcludeVersionMatcher("0.1.0".toVersion()), "!0.1.0".toVersionMatcher())

        assertEquals(MajorMinorVersionPrefixMatcher(1893, 12), "1893.12.+".toVersionMatcher())
        assertEquals(MajorVersionPrefixMatcher(26), "26.+".toVersionMatcher())

        assertEquals(
            CompositeVersionMatcher(
                listOf(
                    GreaterThanOrEqualVersionMatcher("0.1.0".toVersion()),
                    LessThanOrEqualVersionMatcher("0.1.0".toVersion())
                )
            ), ">=0.1.0, <=0.1.0".toVersionMatcher()
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