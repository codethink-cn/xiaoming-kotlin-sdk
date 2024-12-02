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

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Put the value into the map if absent, or remove the old value and return.
 */
private fun <K, V> MutableMap<K, V>.putSingly(key: K, value: V): V? {
    val oldValue = putIfAbsent(key, value)
    if (oldValue != null) {
        remove(key)
    }
    return oldValue
}

interface DualKeyMap<K1, K2, V> {
    data class Key<K1, K2>(val key1: K1, val key2: K2)
    data class Entry<K1, K2, V>(val key: Key<K1, K2>, val value: V)

    operator fun get(key: Key<K1, K2>): V?
    operator fun get(key1: K1, key2: K2): V?

    fun containsKey(key: Key<K1, K2>): Boolean
    operator fun contains(key: Key<K1, K2>): Boolean

    fun containsKey1(key1: K1): Boolean
    fun containsKey2(key2: K2): Boolean

    fun toMap(): Map<Key<K1, K2>, V>
    fun toValues(): Collection<V>

    fun toMapByKey2(key2: K2): Map<K1, V>
    fun associatedByKey2(key2: K2): Map<K1, List<V>>

    fun toMapByKey1(key1: K1): Map<K2, V>
    fun associatedByKey1(key1: K1): Map<K2, List<V>>

    fun singleOrNullByKey1(key1: K1): V?
    fun singleOrNullByKey2(key2: K2): V?

    val size: Int
    val isEmpty: Boolean
}

val DualKeyMap<*, *, *>.isNotEmpty: Boolean
    get() = !isEmpty

@InternalApi
class MutableDualKeyMap<K1, K2, V> : DualKeyMap<K1, K2, V> {
    private val lock = ReentrantReadWriteLock()

    private val key1: MutableSet<K1> = HashSet()
    private val key2: MutableSet<K2> = HashSet()

    private val values: MutableMap<DualKeyMap.Key<K1, K2>, V> = HashMap()

    // If null, means the value is not cached.
    private val entriesByKey1: MutableMap<K1, DualKeyMap.Entry<K1, K2, V>> = HashMap()
    private val entriesByKey2: MutableMap<K2, DualKeyMap.Entry<K1, K2, V>> = HashMap()

    fun put(key1: K1, key2: K2, value: V): V? = put(DualKeyMap.Key(key1, key2), value)
    operator fun set(key1: K1, key2: K2, value: V): V? = put(key1, key2, value)

    fun put(key: DualKeyMap.Key<K1, K2>, value: V): V? = lock.write {
        key1.add(key.key1)
        key2.add(key.key2)

        val entry = DualKeyMap.Entry(key, value)

        entriesByKey1.putSingly(key.key1, entry)
        entriesByKey2.putSingly(key.key2, entry)

        return values.put(key, value)
    }

    operator fun set(key: DualKeyMap.Key<K1, K2>, value: V): V? = put(key, value)

    fun putIfAbsent(key: DualKeyMap.Key<K1, K2>, value: V): V? = lock.write {
        if (key in values) {
            return@write values[key]
        }
        return@write put(key, value)
    }

    fun putIfAbsent(key1: K1, key2: K2, value: V): V? = putIfAbsent(DualKeyMap.Key(key1, key2), value)

    fun remove(key1: K1, key2: K2): V? = remove(DualKeyMap.Key(key1, key2))
    fun remove(key: DualKeyMap.Key<K1, K2>): V? = lock.write {
        val oldValue = values.remove(key) ?: return@write null

        entriesByKey1.remove(key.key1)
        entriesByKey2.remove(key.key2)

        this.key1.remove(key.key1)
        this.key2.remove(key.key2)

        return@write oldValue
    }

    override operator fun get(key1: K1, key2: K2): V? = lock.read { values[DualKeyMap.Key(key1, key2)] }

    override fun get(key: DualKeyMap.Key<K1, K2>): V? = lock.read { values[key] }

    override fun containsKey(key: DualKeyMap.Key<K1, K2>): Boolean = lock.read { key in values }

    override operator fun contains(key: DualKeyMap.Key<K1, K2>): Boolean = containsKey(key)

    override fun containsKey1(key1: K1): Boolean = lock.read { key1 in this.key1 }

    override fun containsKey2(key2: K2): Boolean = lock.read { key2 in this.key2 }

    override fun toMap(): Map<DualKeyMap.Key<K1, K2>, V> = lock.read { values.toMap() }

    override fun toMapByKey2(key2: K2): Map<K1, V> = lock.read {
        if (key2 !in this.key2) {
            return@read emptyMap()
        }
        entriesByKey2[key2]?.let { return@read mapOf(it.key.key1 to it.value) }
        return@read values.filterKeys { it.key2 == key2 }.mapKeys { it.key.key1 }
    }

    override fun associatedByKey1(key1: K1): Map<K2, List<V>> = lock.read {
        if (key1 !in this.key1) {
            return@read emptyMap()
        }
        entriesByKey1[key1]?.let { return@read mapOf(it.key.key2 to listOf(it.value)) }
        return@read values.entries.filter { it.key.key1 == key1 }.groupBy({ it.key.key2 }, { it.value })
    }

    override fun toMapByKey1(key1: K1): Map<K2, V> = lock.read {
        if (key1 !in this.key1) {
            return@read emptyMap()
        }
        entriesByKey1[key1]?.let { return@read mapOf(it.key.key2 to it.value) }
        return@read values.filterKeys { it.key1 == key1 }.mapKeys { it.key.key2 }
    }

    override fun associatedByKey2(key2: K2): Map<K1, List<V>> = lock.read {
        if (key2 !in this.key2) {
            return@read emptyMap()
        }
        entriesByKey2[key2]?.let { return@read mapOf(it.key.key1 to listOf(it.value)) }
        return@read values.entries.filter { it.key.key2 == key2 }.groupBy({ it.key.key1 }, { it.value })
    }

    override fun toValues(): Collection<V> = lock.read { values.values }

    override fun singleOrNullByKey1(key1: K1): V? = lock.read { entriesByKey1[key1]?.value }

    override fun singleOrNullByKey2(key2: K2): V? = lock.read { entriesByKey2[key2]?.value }

    override val size: Int
        get() = lock.read { values.size }

    override val isEmpty: Boolean
        get() = lock.read { values.isEmpty() }
}

