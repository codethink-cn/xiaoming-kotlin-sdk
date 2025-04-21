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
                MinorityOptionalWildCardStringMatcher,
                "1919810".toLiteralStringMatcher()
            ).toSegmentIdMatcher().matches("114514.1919810".toSegmentId())
        )
        assertFalse(
            listOf(
                "114514".toLiteralStringMatcher(),
                MinorityRequiredWildCardStringMatcher,
                "1919810".toLiteralStringMatcher()
            ).toSegmentIdMatcher().matches("114514.1919810".toSegmentId())
        )

        assertTrue(
            listOf(
                "18\\d+".toRegexStringMatcher(),
                MajorityRequiredWildCardStringMatcher
            ).toSegmentIdMatcher().matches("1893.12.26".toSegmentId())
        )
        assertTrue(
            listOf(
                "18\\d+".toRegexStringMatcher(),
                MajorityOptionalWildCardStringMatcher
            ).toSegmentIdMatcher().matches("1893.12.26".toSegmentId())
        )

        assertFalse(
            listOf(
                "18".toLiteralStringMatcher(),
                AnyStringMatcher,
                AnyStringMatcher
            ).toSegmentIdMatcher().matches("1893.12.26".toSegmentId())
        )

        assertThrows<IllegalArgumentException> {
            listOf(
                "1893".toLiteralStringMatcher(),
                MajorityRequiredWildCardStringMatcher,
                AnyStringMatcher
            ).toSegmentIdMatcher().matches("1893.12.26".toSegmentId())
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
                MinorityRequiredWildCardStringMatcher,
                "xiaoming".toLiteralStringMatcher()
            ).toSegmentIdMatcher(), "cn.+.xiaoming".toSegmentIdMatcher()
        )

        assertEquals(
            listOf(
                "cn".toLiteralStringMatcher(),
                MajorityRequiredWildCardStringMatcher,
                "xiaoming".toLiteralStringMatcher()
            ).toSegmentIdMatcher(), "cn.++.xiaoming".toSegmentIdMatcher()
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
            assertTrue(matches("a.b.c".toSegmentId()))
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.b.AAA".toSegmentId()))
            assertTrue(matches("a.b.AAA.BBB".toSegmentId()))
            assertFalse(matches("a".toSegmentId()))
        }

        "a.?".toSegmentIdMatcher().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.c".toSegmentId()))
            assertTrue(matches("a".toSegmentId()))
            assertFalse(matches("a.B.C".toSegmentId()))
        }

        "a.+".toSegmentIdMatcher().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.c".toSegmentId()))
            assertFalse(matches("a".toSegmentId()))
            assertFalse(matches("a.B.C".toSegmentId()))
        }

        "a.{\\\\d}".toSegmentIdMatcher().apply {
            assertFalse(matches("a.b".toSegmentId()))
            assertTrue(matches("a.5".toSegmentId()))
            assertFalse(matches("a".toSegmentId()))
            assertFalse(matches("a.B.C".toSegmentId()))
        }

        "a.++".toSegmentIdMatcher().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.5".toSegmentId()))
            assertFalse(matches("a".toSegmentId()))
            assertTrue(matches("a.B.C".toSegmentId()))
        }

        "a.??".toSegmentIdMatcher().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.5".toSegmentId()))
            assertTrue(matches("a".toSegmentId()))
            assertTrue(matches("a.B.C".toSegmentId()))
        }

        "a.??.b".toSegmentIdMatcher().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.5.b".toSegmentId()))
            assertTrue(matches("a.AA.BB.b".toSegmentId()))
            assertFalse(matches("a.B.C".toSegmentId()))
            assertTrue(matches("a.B.b".toSegmentId()))
        }
    }
}