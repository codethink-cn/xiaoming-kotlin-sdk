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

interface DualKeyMap<K1, K2, out V> : Iterable<DualKeyMap.Entry<K1, K2, V>> {
    interface Key<K1, K2> {
        val key1: K1
        val key2: K2
    }

    interface Entry<K1, K2, out V> {
        val key: Key<K1, K2>
        val value: V
    }

    val size: Int
    val isEmpty: Boolean

    operator fun get(key: Key<K1, K2>): V?
    operator fun get(key1: K1, key2: K2): V?

    fun containsKey(key: Key<K1, K2>): Boolean
    operator fun contains(key: Key<K1, K2>): Boolean

    fun containsKey1(key1: K1): Boolean
    fun containsKey2(key2: K2): Boolean

    fun toMap(): Map<Key<K1, K2>, V>

    val values: Collection<V>
    val entries: Collection<Entry<K1, K2, @UnsafeVariance V>>

    fun toMapByKey2(key2: K2): Map<K1, V>
    fun associatedByKey2(key2: K2): Map<K1, List<V>>

    fun toMapByKey1(key1: K1): Map<K2, V>
    fun associatedByKey1(key1: K1): Map<K2, List<V>>

    fun singleOrNullByKey1(key1: K1): V?
    fun singleOrNullByKey2(key2: K2): V?
}

@InternalApi
val DualKeyMap<*, *, *>.isNotEmpty: Boolean
    get() = !isEmpty
