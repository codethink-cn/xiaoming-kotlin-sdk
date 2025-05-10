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

import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.concurrent.read
import kotlin.concurrent.write

@InternalApi
class MutableDualKeyMapImpl<F, S, V> private constructor(
    private val dataByKey: MutableMap<Pair<F, S>, V>,
    private val dataByFirstKey: MutableMap<F, MutableMap<S, V>>
) : MutableDualKeyMap<F, S, V> {
    private val lock = ReentrantReadWriteLock()

    override val firstKeys: MutableSet<F> get() = dataByFirstKey.keys
    override val firstEntries: MutableSet<MutableMap.MutableEntry<F, MutableMap<S, V>>> get() = dataByFirstKey.entries

    override val entries: MutableSet<MutableMap.MutableEntry<Pair<F, S>, V>> get() = dataByKey.entries

    override val keys: MutableSet<Pair<F, S>> get() = dataByKey.keys

    override val size: Int get() = dataByKey.size

    override val values: MutableCollection<V> get() = dataByKey.values

    constructor() : this(mutableMapOf(), mutableMapOf())

    override fun containsKey(key: Pair<F, S>): Boolean {
        return lock.read {
            dataByKey.containsKey(key)
        }
    }

    override fun containsKey(firstKey: F, secondKey: S): Boolean {
        return lock.read {
            dataByFirstKey[firstKey]?.containsKey(secondKey) ?: false
        }
    }

    override fun get(key: Pair<F, S>): V? {
        return lock.read {
            dataByKey[key]
        }
    }

    override fun get(firstKey: F): Map<S, V> {
        return lock.read {
            dataByFirstKey.getOrDefault(firstKey, emptyMap())
        }
    }

    override fun get(firstKey: F, secondKey: S): V? {
        return lock.read {
            dataByFirstKey[firstKey]?.get(secondKey)
        }
    }

    override fun put(key: Pair<F, S>, value: V): V? {
        return lock.write {
            dataByFirstKey.getOrPut(key.first) { mutableMapOf() }[key.second] = value
            dataByKey.put(key, value)
        }
    }

    override fun put(firstKey: F, secondKey: S, value: V): V? {
        return put(firstKey to secondKey, value)
    }

    override fun remove(key: Pair<F, S>): V? {
        return lock.write {
            val value = dataByKey.remove(key)
            if (value != null) {
                val valuesByFirstKey = dataByFirstKey[key.first]
                checkNotNull(valuesByFirstKey) { "Key $key not found in dataByFirstKey" }

                valuesByFirstKey.remove(key.second)
                if (valuesByFirstKey.isEmpty()) {
                    dataByFirstKey.remove(key.first)
                }
            }
            value
        }
    }

    override fun remove(firstKey: F, secondKey: S): V? {
        return remove(firstKey to secondKey)
    }

    override fun putIfAbsent(firstKey: F, secondKey: S, value: V): V? {
        return putIfAbsent(firstKey to secondKey, value)
    }

    override fun computeIfAbsent(firstKey: F, secondKey: S, mappingFunction: Function<in Pair<F, S>, out V>): V {
        return computeIfAbsent(firstKey to secondKey, mappingFunction)
    }

    override fun computeIfAbsent(key: Pair<F, S>, mappingFunction: Function<in Pair<F, S>, out V>): V {
        return lock.write { super.computeIfAbsent(key, mappingFunction) }
    }

    override fun computeIfPresent(key: Pair<F, S>, remappingFunction: BiFunction<in Pair<F, S>, in V & Any, out V?>): V? {
        return lock.write { super.computeIfPresent(key, remappingFunction) }
    }

    override fun computeIfPresent(firstKey: F, secondKey: S, remappingFunction: BiFunction<in Pair<F, S>, in V & Any, out V?>): V? {
        return computeIfPresent(firstKey to secondKey, remappingFunction)
    }

    override fun clone(): MutableDualKeyMap<F, S, V> {
        return lock.read {
            MutableDualKeyMapImpl(
                dataByKey.toMutableMap(),
                dataByFirstKey.mapValues { it.value.toMutableMap() }.toMutableMap()
            )
        }
    }

    override fun iterator(): Iterator<Map.Entry<Pair<F, S>, V>> {
        return lock.read {
            dataByKey.entries.iterator()
        }
    }

    override fun isEmpty(): Boolean {
        return lock.read {
            dataByKey.isEmpty()
        }
    }

    override fun clear() {
        lock.write {
            dataByKey.clear()
            dataByFirstKey.clear()
        }
    }

    override fun putAll(from: Map<out Pair<F, S>, V>) {
        lock.write {
            from.forEach { (key, value) ->
                dataByKey[key] = value
                dataByFirstKey.getOrPut(key.first) { mutableMapOf() }[key.second] = value
            }
        }
    }

    override fun putAllFromFirstMap(map: Map<F, Map<S, V>>) {
        lock.write {
            map.forEach { (firstKey, secondMap) ->
                val valuesByFirstKey = dataByFirstKey.getOrPut(firstKey) { mutableMapOf() }
                secondMap.forEach { (secondKey, value) ->
                    valuesByFirstKey[secondKey] = value
                    dataByKey[firstKey to secondKey] = value
                }
            }
        }
    }

    override fun containsValue(value: V): Boolean {
        return lock.read {
            dataByKey.containsValue(value)
        }
    }
}