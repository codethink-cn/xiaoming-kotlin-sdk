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
import org.junit.jupiter.api.Test

class ListsTest {
    @Test
    @OptIn(InternalApi::class)
    fun testProducts() {
        val a = mutableListOf(1, 2, 3)
        val b = mutableListOf(10, 20, 30)
        val c = mutableListOf(100, 200, 300)

        assertEquals(
            listOf(
                listOf(1, 10, 100),
                listOf(1, 10, 200),
                listOf(1, 10, 300),
                listOf(1, 20, 100),
                listOf(1, 20, 200),
                listOf(1, 20, 300),
                listOf(1, 30, 100),
                listOf(1, 30, 200),
                listOf(1, 30, 300),
                listOf(2, 10, 100),
                listOf(2, 10, 200),
                listOf(2, 10, 300),
                listOf(2, 20, 100),
                listOf(2, 20, 200),
                listOf(2, 20, 300),
                listOf(2, 30, 100),
                listOf(2, 30, 200),
                listOf(2, 30, 300),
                listOf(3, 10, 100),
                listOf(3, 10, 200),
                listOf(3, 10, 300),
                listOf(3, 20, 100),
                listOf(3, 20, 200),
                listOf(3, 20, 300),
                listOf(3, 30, 100),
                listOf(3, 30, 200),
                listOf(3, 30, 300)
            ),
            listOf(a, b, c).products().toList()
        )

        assertEquals(
            listOf(
                listOf(1, 10),
                listOf(1, 20),
                listOf(1, 30),
                listOf(2, 10),
                listOf(2, 20),
                listOf(2, 30),
                listOf(3, 10),
                listOf(3, 20),
                listOf(3, 30)
            ),
            listOf(a, b).products().toList()
        )

        val d = listOf(-1, -2)
        val f = listOf(-2)

        assertEquals(
            listOf(
                listOf(-1, -2),
                listOf(-2, -2)
            ),
            listOf(d, f).products().toList()
        )
    }
}