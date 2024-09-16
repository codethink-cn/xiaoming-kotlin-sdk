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
import cn.codethink.xiaoming.common.MinorityRequiredWildcardStringMatcher
import cn.codethink.xiaoming.common.RegexStringMatcher
import cn.codethink.xiaoming.common.segmentIdOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SegmentIdTest {
    @Test
    fun testParseSegmentId() {
        val packageId = segmentIdOf("cn.codethink.xiaoming.io.common")
        assertEquals(listOf("cn", "codethink", "xiaoming", "io", "common"), packageId.toList())
    }

    @Test
    fun testDefaultStringListMatcher() {
        // Empty list.
        assertThrows<IllegalArgumentException> { DefaultSegmentIdMatcher(emptyList()) }

        assertTrue(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("114514"),
                    MinorityOptionalWildcardStringMatcher,
                    LiteralStringMatcher("1919810")
                )
            ).isMatched(segmentIdOf("114514.1919810"))
        )
        assertFalse(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("114514"),
                    MinorityRequiredWildcardStringMatcher,
                    LiteralStringMatcher("1919810")
                )
            ).isMatched(segmentIdOf("114514.1919810"))
        )

        assertTrue(
            DefaultSegmentIdMatcher(
                listOf(
                    RegexStringMatcher("18\\d+".toRegex()),
                    MajorityRequiredWildcardStringMatcher
                )
            ).isMatched(segmentIdOf("1893.12.26"))
        )
        assertTrue(
            DefaultSegmentIdMatcher(
                listOf(
                    RegexStringMatcher("18\\d+".toRegex()),
                    MajorityOptionalWildcardStringMatcher
                )
            ).isMatched(segmentIdOf("1893.12.26"))
        )

        assertFalse(
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("18"),
                    AnyMatcher<String>(),
                    AnyMatcher<String>()
                )
            ).isMatched(segmentIdOf("1893.12.26"))
        )

        assertThrows<IllegalArgumentException> {
            DefaultSegmentIdMatcher(
                listOf(
                    LiteralStringMatcher("1893"),
                    MajorityRequiredWildcardStringMatcher,
                    AnyMatcher<String>()
                )
            ).isMatched(segmentIdOf("1893.12.26"))
        }
    }
}
