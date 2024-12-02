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
import java.lang.reflect.Type

/**
 * 通过 [MutableMap] 存储数据的 [Raw]。
 *
 * @author Chuanwise
 */
@JsonSerialize(using = MapRawSerializer::class)
class MapRaw(
    val map: MutableMap<String, Any?> = HashMap()
) : Raw {
    override val keys: Iterable<String>
        get() = map.keys

    override val isEmpty: Boolean
        get() = map.isEmpty()

    override fun get(
        name: String,
        type: Type,
        optional: Boolean,
        nullable: Boolean,
        convertable: Boolean,
        defaultValueFactory: (() -> Any?)?
    ): Any? {
        val value = map[name]
        if (value == null) {
            if (map.containsKey(name)) {
                if (nullable) {
                    return null
                }
                throw NullPointerException("Field $name is not nullable, but found null in $this.")
            }
            if (optional) {
                if (defaultValueFactory != null || nullable) {
                    return defaultValueFactory?.invoke()
                }
                throw NoSuchElementException(
                    "Field $name is optional and not nullable, but not found in $this and default value is null."
                )
            } else {
                throw IllegalArgumentException("Field $name is required and not nullable, but not found in $this.")
            }
        }
        return value
    }

    override operator fun set(name: String, value: Any?) {
        map[name] = value
    }

    override fun contains(key: String): Boolean = key in map

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapRaw) return false

        if (map != other.map) return false
        return true
    }

    override fun contentEquals(raw: Raw): Boolean {
        if (raw is MapRaw) {
            return map == raw.map
        } else if (keys.asSetEquals(raw.keys)) {
            for (key in keys) {
                if (get<Any?>(key) != raw.get<Any?>(key)) {
                    return false
                }
            }
            return true
        } else {
            return false
        }
    }

    override fun hashCode(): Int = map.hashCode()

    override fun toString(): String = "MapRaw(${map.entries.joinToString(", ")})"

    override fun contentToString(): String = map.entries.joinToString(", ")
}

object MapRawSerializer : StdSerializer<MapRaw>(MapRaw::class.java) {
    private fun readResolve(): Any = MapRawSerializer
    override fun serialize(value: MapRaw?, generator: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            generator.writeNull()
        } else {
            generator.writeObject(value.map)
        }
    }
}