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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.util.function.Supplier

@JsonSerialize(using = MapRawSerializer::class)
class MapStoreImpl(
    private val map: MutableMap<String, Any?> = HashMap()
) : AbstractStore(), MutableStore {
    override val keys: Set<String> get() = map.keys
    override val size: Int get() = map.size

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(name: String, meta: TypeMeta<T>, defaultValueFactory: Supplier<T>?): T {
        val value = map[name]

        val valueIsNull = value == null && map.containsKey(name)
        val valuePresent = value != null || valueIsNull
        if (valuePresent) {
            if (valueIsNull && !meta.nullable) {
                throw NullPointerException("The value of key $name is null, but it is not nullable.")
            }
            return value as T
        }

        defaultValueFactory ?: throw NoSuchElementException("No value found for key $name.")
        return defaultValueFactory.get()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrNull(name: String, meta: TypeMeta<T>, defaultValueFactory: Supplier<T>?): T? {
        val value = map[name]

        val valueIsNull = value == null && map.containsKey(name)
        val valuePresent = value != null || valueIsNull
        if (valuePresent) {
            return value as T
        }

        return defaultValueFactory?.get()
    }

    override operator fun set(name: String, value: Any?) {
        map[name] = value
    }

    internal fun toMap(): MutableMap<String, Any?> = map

    override fun contains(key: String): Boolean = key in map

    override fun remove(name: String) {
        map.remove(name)
    }

    override fun clear() {
        map.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MapStoreImpl) {
            return false
        }
        return map == other.map
    }

    override fun hashCode(): Int = map.hashCode()

    override fun toString(): String = "MapRaw(${map.entries.joinToString(", ")})"

    override fun contentToString(): String = map.entries.joinToString(", ")
}

object MapRawSerializer : StdSerializer<MapStoreImpl>(MapStoreImpl::class.java) {
    private fun readResolve(): Any = MapRawSerializer
    override fun serialize(value: MapStoreImpl?, generator: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            generator.writeNull()
        } else {
            generator.writeObject(value.toMap())
        }
    }
}