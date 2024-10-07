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

import cn.codethink.xiaoming.common.AnyMatcher
import cn.codethink.xiaoming.common.DefaultSegmentIdMatcher
import cn.codethink.xiaoming.common.LiteralStringMatcher
import cn.codethink.xiaoming.common.MajorityOptionalWildcardStringMatcher
import cn.codethink.xiaoming.common.MajorityRequiredWildcardStringMatcher
import cn.codethink.xiaoming.common.MinorityOptionalWildcardStringMatcher
import cn.codethink.xiaoming.common.MinorityRequiredOnceWildcardStringMatcher
import cn.codethink.xiaoming.common.MinorityRequiredWildcardStringMatcher
import cn.codethink.xiaoming.common.RegexStringMatcher
import cn.codethink.xiaoming.common.WildcardStringMatcher
import cn.codethink.xiaoming.common.compileSegmentIdMatcher
import cn.codethink.xiaoming.common.toSegmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SegmentIdTest {
    @Test
    fun testParseSegmentId() {
        val packageId = "cn.codethink.xiaoming.io.common".toSegmentId()
        assertEquals(listOf("cn", "codethink", "xiaoming", "io", "common"), packageId.toList())
    }

    @Test
    fun testDefaultSegmentIdMatcher() {
        // Empty list.
        assertThrows<IllegalArgumentException> { DefaultSegmentIdMatcher(emptyList()) }

        assertTrue(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("114514"),
                    MinorityOptionalWildcardStringMatcher,
                    LiteralStringMatcher("1919810")
                )
            ).isMatched("114514.1919810".toSegmentId())
        )
        assertFalse(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("114514"),
                    MinorityRequiredWildcardStringMatcher,
                    LiteralStringMatcher("1919810")
                )
            ).isMatched("114514.1919810".toSegmentId())
        )

        assertTrue(
            DefaultSegmentIdMatcher(
                listOf(
                    RegexStringMatcher("18\\d+".toRegex()),
                    MajorityRequiredWildcardStringMatcher
                )
            ).isMatched("1893.12.26".toSegmentId())
        )
        assertTrue(
            DefaultSegmentIdMatcher(
                listOf(
                    RegexStringMatcher("18\\d+".toRegex()),
                    MajorityOptionalWildcardStringMatcher
                )
            ).isMatched("1893.12.26".toSegmentId())
        )

        assertFalse(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("18"),
                    AnyMatcher<String>(),
                    AnyMatcher<String>()
                )
            ).isMatched("1893.12.26".toSegmentId())
        )

        assertThrows<IllegalArgumentException> {
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("1893"),
                    MajorityRequiredWildcardStringMatcher,
                    AnyMatcher<String>()
                )
            ).isMatched("1893.12.26".toSegmentId())
        }
    }

    @Test
    fun testCompileSegmentIdMatcher() {
        assertEquals(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("cn"),
                    LiteralStringMatcher("codethink"),
                    LiteralStringMatcher("xiaoming")
                )
            ), compileSegmentIdMatcher("cn.codethink.xiaoming")
        )

        assertEquals(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("cn"),
                    RegexStringMatcher("codethink".toRegex()),
                    LiteralStringMatcher("xiaoming")
                )
            ), compileSegmentIdMatcher("cn.{codethink}.xiaoming")
        )

        assertEquals(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("cn"),
                    MinorityRequiredOnceWildcardStringMatcher,
                    LiteralStringMatcher("xiaoming")
                )
            ), compileSegmentIdMatcher("cn.+.xiaoming")
        )

        assertEquals(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("cn"),
                    MinorityRequiredWildcardStringMatcher,
                    LiteralStringMatcher("xiaoming")
                )
            ), compileSegmentIdMatcher("cn.++.xiaoming")
        )

        assertEquals(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("cn"),
                    WildcardStringMatcher.of(majority = false, optional = false, count = 5),
                    LiteralStringMatcher("xiaoming")
                )
            ), compileSegmentIdMatcher("cn.5++.xiaoming")
        )

        assertEquals(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("cn"),
                    MajorityRequiredWildcardStringMatcher,
                    LiteralStringMatcher("xiaoming")
                )
            ), compileSegmentIdMatcher("cn.+++.xiaoming")
        )

        assertThrows<IllegalArgumentException> {
            compileSegmentIdMatcher("cn..xiaoming")
        }

        assertThrows<IllegalArgumentException> {
            compileSegmentIdMatcher("cn.xiaoming.")
        }
    }

    @Test
    fun testDefaultSegmentIdIsMatched() {
        compileSegmentIdMatcher("a.b.*").apply {
            assertTrue(isMatched("a.b.c".toSegmentId()))
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.b.AAA".toSegmentId()))
            assertTrue(isMatched("a.b.AAA.BBB".toSegmentId()))
            assertFalse(isMatched("a".toSegmentId()))
        }

        compileSegmentIdMatcher("a.?").apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.c".toSegmentId()))
            assertTrue(isMatched("a".toSegmentId()))
            assertFalse(isMatched("a.B.C".toSegmentId()))
        }

        compileSegmentIdMatcher("a.+").apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.c".toSegmentId()))
            assertFalse(isMatched("a".toSegmentId()))
            assertFalse(isMatched("a.B.C".toSegmentId()))
        }

        compileSegmentIdMatcher("a.{\\\\d}").apply {
            assertFalse(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.5".toSegmentId()))
            assertFalse(isMatched("a".toSegmentId()))
            assertFalse(isMatched("a.B.C".toSegmentId()))
        }

        compileSegmentIdMatcher("a.++").apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.5".toSegmentId()))
            assertFalse(isMatched("a".toSegmentId()))
            assertTrue(isMatched("a.B.C".toSegmentId()))
        }

        compileSegmentIdMatcher("a.??").apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.5".toSegmentId()))
            assertTrue(isMatched("a".toSegmentId()))
            assertTrue(isMatched("a.B.C".toSegmentId()))
        }

        compileSegmentIdMatcher("a.??.b").apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.5.b".toSegmentId()))
            assertTrue(isMatched("a.AA.BB.b".toSegmentId()))
            assertFalse(isMatched("a.B.C".toSegmentId()))
            assertTrue(isMatched("a.B.b".toSegmentId()))
        }
    }
}
