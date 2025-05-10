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

import java.util.function.BiFunction
import java.util.function.Function

@InternalApi
interface MutableDualKeyMap<F, S, V> : DualKeyMap<F, S, V>, MutableMap<Pair<F, S>, V> {
    override val firstKeys: MutableSet<F>
    override val firstEntries: MutableSet<MutableMap.MutableEntry<F, MutableMap<S, V>>>

    fun put(firstKey: F, secondKey: S, value: V): V?
    operator fun set(firstKey: F, secondKey: S, value: V): V? = put(firstKey, secondKey, value)

    fun putIfAbsent(firstKey: F, secondKey: S, value: V): V?

    fun computeIfAbsent(firstKey: F, secondKey: S, mappingFunction: Function<in Pair<F, S>, out V>): V
    fun computeIfPresent(firstKey: F, secondKey: S, remappingFunction: BiFunction<in Pair<F, S>, in V & Any, out V?>): V?

    fun remove(firstKey: F, secondKey: S): V?
    override fun clone(): MutableDualKeyMap<F, S, V>

    fun putAllFromFirstMap(map: Map<F, Map<S, V>>)
}