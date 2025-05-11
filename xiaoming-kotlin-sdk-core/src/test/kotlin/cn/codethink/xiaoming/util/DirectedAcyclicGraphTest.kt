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

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(InternalApi::class, DelicateCoroutinesApi::class)
class DirectedAcyclicGraphTest {
    private fun <T> MutableDirectedAcyclicGraph<T, Unit>.traverseToList(): List<T> {
        val result = mutableListOf<T>()
        forEach { result.add(it.value) }
        return result
    }

    private fun <T> List<T>.assertRelativeLocation(front: T, back: T) {
        val frontIndex = indexOf(front)
        val backIndex = indexOf(back)
        if (frontIndex == -1 || backIndex == -1) {
            throw IllegalArgumentException("List does not contain $front or $back")
        }
        if (frontIndex > backIndex) {
            throw IllegalArgumentException("$front is not before $back")
        }
    }

    @Test
    fun testDAG(): Unit = runBlocking {
        val graph = MutableDirectedAcyclicGraphImpl<String, Unit>()

        val a = graph.allocate("A")
        val b = graph.allocate("B")
        val c = graph.allocate("C")
        val d = graph.allocate("D")
        val e = graph.allocate("E")
        val f = graph.allocate("F")
        val g = graph.allocate("G")

        val zero = graph.allocate("0")
        val one = graph.allocate("1")
        val two = graph.allocate("2")

        graph.link(two, one)
        graph.link(two, zero)
        graph.link(one, c)
        graph.link(zero, c)

        graph.link(a, b)
        graph.link(a, c)
        graph.link(b, d)
        graph.link(c, d)
        graph.link(c, e)
        graph.link(d, f)
        graph.link(e, f)
        graph.link(d, g)

        assertThrows<IllegalArgumentException> { graph.link(f, a) }
        assertThrows<IllegalArgumentException> { graph.link(g, b) }
        assertThrows<IllegalArgumentException> { graph.link(g, c) }
        assertThrows<IllegalArgumentException> { graph.link(g, d) }

        val traverse = graph.traverseToList()

        traverse.assertRelativeLocation("A", "B")
        traverse.assertRelativeLocation("A", "C")
        traverse.assertRelativeLocation("B", "D")
        traverse.assertRelativeLocation("C", "D")
        traverse.assertRelativeLocation("C", "E")
        traverse.assertRelativeLocation("D", "F")
        traverse.assertRelativeLocation("E", "F")
        traverse.assertRelativeLocation("D", "G")

        graph.forEachConcurrently {
            println(it)
            delay(2000)
            println("$it done")
        }
    }
}