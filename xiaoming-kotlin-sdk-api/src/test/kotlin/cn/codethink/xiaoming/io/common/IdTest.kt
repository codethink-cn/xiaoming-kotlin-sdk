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

import cn.codethink.xiaoming.common.segmentIdOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdTest {
    @Test
    fun testParseSegmentId() {
        val packageId = segmentIdOf("cn.codethink.xiaoming.io.common")
        assertEquals(listOf("cn", "codethink", "xiaoming", "io", "common"), packageId.toList())
    }
}
