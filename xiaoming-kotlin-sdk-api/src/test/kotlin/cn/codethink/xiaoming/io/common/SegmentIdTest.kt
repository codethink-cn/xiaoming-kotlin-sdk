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

import cn.codethink.xiaoming.common.DefaultStringListMatcher
import cn.codethink.xiaoming.common.LiteralStringMatcher
import cn.codethink.xiaoming.common.MajorityOptionalGreedilyWildcardStringMatcher
import cn.codethink.xiaoming.common.MajorityRequiredGreedilyWildcardStringMatcher
import cn.codethink.xiaoming.common.MinorityOptionalGreedilyWildcardStringMatcher
import cn.codethink.xiaoming.common.MinorityRequiredGreedilyWildcardStringMatcher
import cn.codethink.xiaoming.common.RegexStringMatcher
import cn.codethink.xiaoming.common.WildcardStringMatcher
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
        assertThrows<IllegalArgumentException> { DefaultStringListMatcher(emptyList()) }

        assertTrue(
            DefaultStringListMatcher(
                listOf(
                    LiteralStringMatcher("114514"),
                    MinorityOptionalGreedilyWildcardStringMatcher,
                    LiteralStringMatcher("1919810")
                )
            ).isMatched(segmentIdOf("114514.1919810"))
        )
        assertFalse(
            DefaultStringListMatcher(
                listOf(
                    LiteralStringMatcher("114514"),
                    MinorityRequiredGreedilyWildcardStringMatcher,
                    LiteralStringMatcher("1919810")
                )
            ).isMatched(segmentIdOf("114514.1919810"))
        )

        assertTrue(
            DefaultStringListMatcher(
                listOf(
                    RegexStringMatcher("18\\d+".toRegex()),
                    MajorityRequiredGreedilyWildcardStringMatcher
                )
            ).isMatched(segmentIdOf("1893.12.26"))
        )
        assertTrue(
            DefaultStringListMatcher(
                listOf(
                    RegexStringMatcher("18\\d+".toRegex()),
                    MajorityOptionalGreedilyWildcardStringMatcher
                )
            ).isMatched(segmentIdOf("1893.12.26"))
        )

        assertFalse(
            DefaultStringListMatcher(
                listOf(
                    LiteralStringMatcher("18"),
                    WildcardStringMatcher,
                    WildcardStringMatcher
                )
            ).isMatched(segmentIdOf("1893.12.26"))
        )

        assertThrows<IllegalArgumentException> {
            DefaultStringListMatcher(
                listOf(
                    LiteralStringMatcher("1893"),
                    MajorityRequiredGreedilyWildcardStringMatcher,
                    WildcardStringMatcher
                )
            ).isMatched(segmentIdOf("1893.12.26"))
        }
    }
}
