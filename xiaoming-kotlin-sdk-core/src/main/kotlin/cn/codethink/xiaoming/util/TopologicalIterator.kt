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

import java.util.function.Function

@InternalApi
class TopologicalIterator<T>(
    parentCounts: Map<T, Int>,
    private val parentsMappingFunction: Function<T, Iterable<T>>
) : Iterator<T> {
    private val parentCounts: MutableMap<T, Int> = parentCounts.toMutableMap()
    private var value: T? = null

    override fun next(): T {
        var value = value
        if (value != null) {
            for (parent in parentsMappingFunction.apply(value)) {
                parentCounts.computeIfPresent(parent) { _, v -> v - 1 }
            }
        }

        val entries = parentCounts.entries
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()

            if (entry.value == 0) {
                iterator.remove()

                value = entry.key
                this.value = entry.key

                return value
            }
        }

        error("Ring detected, remaining counts: $parentCounts.")
    }

    override fun hasNext(): Boolean {
        return parentCounts.isNotEmpty()
    }
}
