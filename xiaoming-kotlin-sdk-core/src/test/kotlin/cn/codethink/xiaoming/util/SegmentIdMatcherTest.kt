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
import org.junit.jupiter.api.assertThrows

class SegmentIdMatcherTest {
    @Test
    fun testDefaultSegmentIdMatcher() {
        // Empty list.
        assertThrows<IllegalArgumentException> { emptyList<StringMatcher>().toSegmentIdMatcher() }

        assertTrue(
            listOf(
                "114514".toLiteralStringMatcher(),
                MinorityOptionalWildcardStringMatcher,
                "1919810".toLiteralStringMatcher()
            ).toSegmentIdMatcher().isMatched("114514.1919810".toSegmentId())
        )
        assertFalse(
            listOf(
                "114514".toLiteralStringMatcher(),
                MinorityRequiredWildcardStringMatcher,
                "1919810".toLiteralStringMatcher()
            ).toSegmentIdMatcher().isMatched("114514.1919810".toSegmentId())
        )

        assertTrue(
            listOf(
                "18\\d+".toRegexStringMatcher(),
                MajorityRequiredWildcardStringMatcher
            ).toSegmentIdMatcher().isMatched("1893.12.26".toSegmentId())
        )
        assertTrue(
            listOf(
                "18\\d+".toRegexStringMatcher(),
                MajorityOptionalWildcardStringMatcher
            ).toSegmentIdMatcher().isMatched("1893.12.26".toSegmentId())
        )

        assertFalse(
            listOf(
                "18".toLiteralStringMatcher(),
                AnyStringMatcher,
                AnyStringMatcher
            ).toSegmentIdMatcher().isMatched("1893.12.26".toSegmentId())
        )

        assertThrows<IllegalArgumentException> {
            listOf(
                "1893".toLiteralStringMatcher(),
                MajorityRequiredWildcardStringMatcher,
                AnyStringMatcher
            ).toSegmentIdMatcher().isMatched("1893.12.26".toSegmentId())
        }
    }

    @Test
    fun testSegmentIdMatcher() {
        assertEquals(
            listOf(
                "cn".toLiteralStringMatcher(),
                "codethink".toLiteralStringMatcher(),
                "xiaoming".toLiteralStringMatcher()
            ).toSegmentIdMatcher(), "cn.codethink.xiaoming".toSegmentIdMatcher()
        )

        assertEquals(
            listOf(
                "cn".toLiteralStringMatcher(),
                "codethink".toRegexStringMatcher(),
                "xiaoming".toLiteralStringMatcher()
            ).toSegmentIdMatcher(), "cn.{codethink}.xiaoming".toSegmentIdMatcher()
        )

        assertEquals(
            listOf(
                "cn".toLiteralStringMatcher(),
                MinorityRequiredOnceWildcardStringMatcher,
                "xiaoming".toLiteralStringMatcher()
            ).toSegmentIdMatcher(), "cn.+.xiaoming".toSegmentIdMatcher()
        )

        assertEquals(
            listOf(
                "cn".toLiteralStringMatcher(),
                MinorityRequiredWildcardStringMatcher,
                "xiaoming".toLiteralStringMatcher()
            ).toSegmentIdMatcher(), "cn.++.xiaoming".toSegmentIdMatcher()
        )

        assertEquals(
            listOf(
                "cn".toLiteralStringMatcher(),
                WildcardStringMatcher.of(majority = false, optional = false, count = 5),
                "xiaoming".toLiteralStringMatcher()
            ).toSegmentIdMatcher(), "cn.5++.xiaoming".toSegmentIdMatcher()
        )

        assertEquals(
            listOf(
                "cn".toLiteralStringMatcher(),
                MajorityRequiredWildcardStringMatcher,
                "xiaoming".toLiteralStringMatcher()
            ).toSegmentIdMatcher(), "cn.+++.xiaoming".toSegmentIdMatcher()
        )

        assertThrows<IllegalArgumentException> {
            "cn..xiaoming".toSegmentIdMatcher()
        }

        assertThrows<IllegalArgumentException> {
            "cn.xiaoming.".toSegmentIdMatcher()
        }
    }

    @Test
    fun testDefaultSegmentIdIsMatched() {
        "a.b.*".toSegmentIdMatcher().apply {
            assertTrue(isMatched("a.b.c".toSegmentId()))
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.b.AAA".toSegmentId()))
            assertTrue(isMatched("a.b.AAA.BBB".toSegmentId()))
            assertFalse(isMatched("a".toSegmentId()))
        }

        "a.?".toSegmentIdMatcher().apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.c".toSegmentId()))
            assertTrue(isMatched("a".toSegmentId()))
            assertFalse(isMatched("a.B.C".toSegmentId()))
        }

        "a.+".toSegmentIdMatcher().apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.c".toSegmentId()))
            assertFalse(isMatched("a".toSegmentId()))
            assertFalse(isMatched("a.B.C".toSegmentId()))
        }

        "a.{\\\\d}".toSegmentIdMatcher().apply {
            assertFalse(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.5".toSegmentId()))
            assertFalse(isMatched("a".toSegmentId()))
            assertFalse(isMatched("a.B.C".toSegmentId()))
        }

        "a.++".toSegmentIdMatcher().apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.5".toSegmentId()))
            assertFalse(isMatched("a".toSegmentId()))
            assertTrue(isMatched("a.B.C".toSegmentId()))
        }

        "a.??".toSegmentIdMatcher().apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.5".toSegmentId()))
            assertTrue(isMatched("a".toSegmentId()))
            assertTrue(isMatched("a.B.C".toSegmentId()))
        }

        "a.??.b".toSegmentIdMatcher().apply {
            assertTrue(isMatched("a.b".toSegmentId()))
            assertTrue(isMatched("a.5.b".toSegmentId()))
            assertTrue(isMatched("a.AA.BB.b".toSegmentId()))
            assertFalse(isMatched("a.B.C".toSegmentId()))
            assertTrue(isMatched("a.B.b".toSegmentId()))
        }
    }
}