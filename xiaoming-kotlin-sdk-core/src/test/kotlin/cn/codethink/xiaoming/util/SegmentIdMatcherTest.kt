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
import org.junit.jupiter.api.assertThrows

class SegmentIdMatcherTest {
    @Test
    fun testMatches() {
        // Empty list.
        assertThrows<IllegalArgumentException> {
            emptyList<SegmentIdPatternElement>().toSegmentIdPattern()
        }

        assertTrue(
            listOf(
                "114514".toLiteralSegmentIdPatternElement(),
                WildCardSegmentIdPatternElement.OPTIONAL,
                "1919810".toLiteralSegmentIdPatternElement()
            ).toSegmentIdPattern().matches("114514.1919810".toSegmentId())
        )

        assertFalse(
            listOf(
                "114514".toLiteralSegmentIdPatternElement(),
                WildCardSegmentIdPatternElement.REQUIRED,
                "1919810".toLiteralSegmentIdPatternElement()
            ).toSegmentIdPattern().matches("114514.1919810".toSegmentId())
        )

        assertTrue(
            listOf(
                "18\\d+".toRegexSegmentIdPatternElement(),
                WildCardSegmentIdPatternElement.GREEDY_REQUIRED
            ).toSegmentIdPattern().matches("1893.12.26".toSegmentId())
        )
        assertTrue(
            listOf(
                "18\\d+".toRegexSegmentIdPatternElement(),
                WildCardSegmentIdPatternElement.GREEDY_OPTIONAL
            ).toSegmentIdPattern().matches("1893.12.26".toSegmentId())
        )

        assertFalse(
            listOf(
                "18".toLiteralSegmentIdPatternElement(),
                WildCardSegmentIdPatternElement.GREEDY_OPTIONAL,
            ).toSegmentIdPattern().matches("1893.12.26".toSegmentId())
        )

        assertThrows<IllegalArgumentException> {
            listOf(
                "1893".toLiteralSegmentIdPatternElement(),
                WildCardSegmentIdPatternElement.GREEDY_REQUIRED,
                WildCardSegmentIdPatternElement.OPTIONAL
            ).toSegmentIdPattern().matches("1893.12.26".toSegmentId())
        }
    }

    @Test
    fun testSerialization() {
        assertEquals(
            listOf(
                "cn".toLiteralSegmentIdPatternElement(),
                "codethink".toLiteralSegmentIdPatternElement(),
                "xiaoming".toLiteralSegmentIdPatternElement()
            ).toSegmentIdPattern(), "cn.codethink.xiaoming".toSegmentIdPattern()
        )

        assertEquals(
            listOf(
                "cn".toLiteralSegmentIdPatternElement(),
                "codethink".toRegexSegmentIdPatternElement(),
                "xiaoming".toLiteralSegmentIdPatternElement()
            ).toSegmentIdPattern(), "cn.{codethink}.xiaoming".toSegmentIdPattern()
        )

        assertEquals(
            listOf(
                "cn".toLiteralSegmentIdPatternElement(),
                WildCardSegmentIdPatternElement.REQUIRED,
                "xiaoming".toLiteralSegmentIdPatternElement()
            ).toSegmentIdPattern(), "cn.+.xiaoming".toSegmentIdPattern()
        )

        assertEquals(
            listOf(
                "cn".toLiteralSegmentIdPatternElement(),
                WildCardSegmentIdPatternElement.GREEDY_REQUIRED,
                "xiaoming".toLiteralSegmentIdPatternElement()
            ).toSegmentIdPattern(), "cn.++.xiaoming".toSegmentIdPattern()
        )

        assertThrows<IllegalArgumentException> {
            "cn..xiaoming".toSegmentIdPattern()
        }

        assertThrows<IllegalArgumentException> {
            "cn.xiaoming.".toSegmentIdPattern()
        }
    }

    @Test
    fun testSerializationAndMatches() {
        "a.b.*".toSegmentIdPattern().apply {
            assertTrue(matches("a.b.c".toSegmentId()))
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.b.AAA".toSegmentId()))
            assertTrue(matches("a.b.AAA.BBB".toSegmentId()))
            assertFalse(matches("a".toSegmentId()))
        }

        "a.?".toSegmentIdPattern().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.c".toSegmentId()))
            assertTrue(matches("a".toSegmentId()))
            assertTrue(matches("a.B.C".toSegmentId()))
        }

        "a.+".toSegmentIdPattern().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.c".toSegmentId()))
            assertFalse(matches("a".toSegmentId()))
            assertTrue(matches("a.B.C".toSegmentId()))
        }

        "a.{\\\\d}".toSegmentIdPattern().apply {
            assertFalse(matches("a.b".toSegmentId()))
            assertTrue(matches("a.5".toSegmentId()))
            assertFalse(matches("a".toSegmentId()))
            assertFalse(matches("a.B.C".toSegmentId()))
        }

        "a.++".toSegmentIdPattern().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.5".toSegmentId()))
            assertFalse(matches("a".toSegmentId()))
            assertTrue(matches("a.B.C".toSegmentId()))
        }

        "a.??".toSegmentIdPattern().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.5".toSegmentId()))
            assertTrue(matches("a".toSegmentId()))
            assertTrue(matches("a.B.C".toSegmentId()))
        }

        "a.??.b".toSegmentIdPattern().apply {
            assertTrue(matches("a.b".toSegmentId()))
            assertTrue(matches("a.5.b".toSegmentId()))
            assertTrue(matches("a.AA.BB.b".toSegmentId()))
            assertFalse(matches("a.B.C".toSegmentId()))
            assertTrue(matches("a.B.b".toSegmentId()))
        }
    }
}