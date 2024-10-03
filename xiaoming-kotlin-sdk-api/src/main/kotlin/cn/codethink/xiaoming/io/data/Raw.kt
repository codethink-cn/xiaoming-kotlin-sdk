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

package cn.codethink.xiaoming.io.data

import cn.codethink.xiaoming.common.Tristate
import cn.codethink.xiaoming.common.defaultNullable
import cn.codethink.xiaoming.common.defaultOptional
import cn.codethink.xiaoming.common.getOrConstruct
import cn.codethink.xiaoming.common.upgrade
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.lang.reflect.Type
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Supplier
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KProperty

/**
 * Specify default value for field.
 *
 * @author Chuanwise
 */
enum class DefaultValue {
    /**
     * The field is required and not-nullable, or exception will be thrown.
     */
    NULL,

    /**
     * Corresponding empty value as default value.
     */
    EMPTY,

    /**
     * No default value provided.
     */
    UNDEFINED
}

/**
 * An annotation to override some default settings of accessing and modifying
 * a delegated field in [Raw] class.
 *
 * @author Chuanwise
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Field(
    /**
     * By default, the name of the field is the same as the name of the property.
     * Use [Field.name] to override it like this:
     *
     * ```kt
     * // read this field by "raw.get("field", String::class.java)"
     * val field: String by raw
     *
     * // read this field by "raw.get("cn.codethink.xiaoming:extension-field", String::class.java)"
     * @Field("cn.codethink.xiaoming:extension-field")
     * val extensionField: String by raw
     * ```
     */
    val name: String = "",

    /**
     * If the type of field `T` is nullable, the default value of [nullable] is `true`.
     * If it's false, don't forget to provide a default value when calling [Raw.get].
     */
    val nullable: Tristate = Tristate.NULL,

    /**
     * If the field is optional, the default value of [optional] is `true`.
     */
    val optional: Tristate = Tristate.NULL,

    /**
     * @see DefaultValue
     */
    val defaultValue: DefaultValue = DefaultValue.UNDEFINED
)

fun Field?.nameOrDefault(defaultValue: String): String = this?.name?.takeIf { it.isNotBlank() } ?: defaultValue
inline fun <reified T> Field?.nullableOrDefault(): Boolean = this?.nullable?.value ?: defaultNullable<T>()
inline fun <reified T> Field?.optionalOrDefault(): Boolean = this?.optional?.value ?: defaultOptional<T>()
inline fun <reified T> Field?.defaultValueFactoryOrNull(): Supplier<T?>? = when (this?.defaultValue) {
    null -> null
    DefaultValue.NULL -> {
        null
    }

    DefaultValue.EMPTY, DefaultValue.UNDEFINED -> {
        if (defaultValue == DefaultValue.UNDEFINED && (optional.value == false || (nullable.value == true))) {
            null
        } else {
            Supplier {
                if (T::class.java.isArray) {
                    java.lang.reflect.Array.newInstance(T::class.java.componentType, 0) as T
                }

                @Suppress("IMPLICIT_CAST_TO_ANY")
                when (T::class) {
                    String::class -> ""
                    Int::class -> 0
                    Long::class -> 0L
                    Short::class -> 0.toShort()
                    Byte::class -> 0.toByte()
                    Double::class -> 0.0
                    Float::class -> 0.0f
                    Char::class -> 0.toChar()
                    Boolean::class -> false
                    Map::class -> emptyMap<Nothing, Nothing>()
                    List::class -> emptyList<Nothing>()
                    Set::class -> emptySet<Nothing>()
                    else -> getOrConstruct(T::class.java)
                } as T
            }
        }
    }
}

/**
 * Classes implementing this
 * interface **MUST** support Jackson serialization.
 *
 * @author Chuanwise
 */
interface Raw {
    fun get(
        name: String,
        type: Type,
        optional: Boolean,
        nullable: Boolean,
        defaultValueFactory: Supplier<Any?>? = null
    ): Any?

    fun set(
        name: String,
        value: Any?,
        optional: Boolean,
        nullable: Boolean
    )

    operator fun contains(key: String): Boolean
    val keys: Iterable<String>
    val isEmpty: Boolean

    fun contentEquals(raw: Raw): Boolean
    fun contentToString(): String
}

inline operator fun <reified T : Any?> Raw.get(name: String): T = get(
    name, object : TypeReference<T>() {}.type,
    optional = defaultOptional<T>(), nullable = defaultNullable<T>()
) as T

inline operator fun <reified T : Any?> Raw.set(name: String, value: T) = set(
    name, value, optional = defaultOptional<T>(), nullable = defaultNullable<T>()
)

inline operator fun <reified T : Any?> Raw.getValue(thisRef: Any?, property: KProperty<*>): T {
    val annotation = property.annotations.firstOrNull { it is Field } as Field?
    return get(
        name = annotation.nameOrDefault(property.name),
        type = object : TypeReference<T>() {}.type,
        optional = annotation.optionalOrDefault<T>(),
        nullable = annotation.nullableOrDefault<T>(),
        defaultValueFactory = { annotation.defaultValueFactoryOrNull<T>() }
    ) as T
}

inline operator fun <reified T : Any?> Raw.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    val annotation = property.annotations.firstOrNull { it is Field } as Field?
    return set(
        name = annotation.nameOrDefault(property.name),
        value = value,
        optional = annotation.optionalOrDefault<T>(),
        nullable = annotation.nullableOrDefault<T>()
    )
}

private fun Iterable<String>.asSetEquals(keys: Iterable<String>): Boolean {
    val set = toSet()
    return set == keys.toSet()
}

/**
 * A [Raw] implementation that stores data in a [ObjectNode] used in deserializers.
 *
 * When accessing fields, it will check [cache] and return the cached value if exists,
 * or deserialize the field from the corresponding item in [ObjectNode] with given type
 * and cache it. When modifying fields, it will update both [cache] and [ObjectNode].
 *
 * Different type caching is supported, which means developers can get same field with
 * different types, and the value will be deserialized and cached properly.
 *
 * Notice that [hashCode] and [equals] methods are implemented based on mutable field [node].
 *
 * This class is thread-safe.
 *
 * @author Chuanwise
 */
@JsonSerialize(using = NodeRawSerializer::class)
class NodeRaw(
    val mapper: ObjectMapper,
    val node: ObjectNode
) : Raw {
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    // Key: field name, Value: null (if value is set to null) or a map of type to value.
    private val cache: MutableMap<String, MutableMap<Type, Any?>?> = mutableMapOf()

    override val keys: Iterable<String>
        get() = node.fieldNames().asSequence().toList()

    override val isEmpty: Boolean
        get() = node.isEmpty

    constructor(
        mapper: ObjectMapper
    ) : this(mapper, mapper.nodeFactory.objectNode())

    override fun get(
        name: String,
        type: Type,
        optional: Boolean,
        nullable: Boolean,
        defaultValueFactory: Supplier<Any?>?
    ): Any? = lock.read {
        // Method to read value from node.
        fun doGetValue(): Any? {
            val javaType = mapper.constructType(type)
            val fieldNode = node[name]
                ?: if (optional) {
                    if (defaultValueFactory != null || nullable) {
                        return defaultValueFactory?.get()
                    }
                    throw NoSuchElementException("Field $name is optional, but not found in $this.")
                } else {
                    throw IllegalArgumentException("Field $name is required and not-nullable, but not found in $this.")
                }

            if (fieldNode is NullNode) {
                if (nullable) {
                    return null
                } else {
                    throw IllegalArgumentException("Field $name is required and not-nullable, but found null in $this.")
                }
            }

            val value: Any? = mapper.readValue(fieldNode.traverse(mapper), javaType)
            if (value == null && !nullable) {
                throw NullPointerException("Field $name is not-nullable, but found null in $this.")
            }

            return value
        }

        val values = cache[name]
        if (values == null) {
            // Value stored in cache is null.
            if (cache.containsKey(name)) {
                if (!nullable) {
                    throw NullPointerException("Field $name is not-nullable, but found null in $this.")
                }
                return@read null
            }

            // Value is not stored.
            val value = doGetValue()
            lock.upgrade {
                if (value == null) {
                    cache[name] = null
                } else {
                    cache[name] = mutableMapOf(type to value)
                }
            }
            return@read value
        }

        var value = values[type]
        if (value == null) {
            value = doGetValue()
            lock.upgrade {
                if (value == null) {
                    cache[name] = null
                } else {
                    values[type] = value
                }
            }
        }
        return@read value
    }

    override fun set(
        name: String,
        value: Any?,
        optional: Boolean,
        nullable: Boolean
    ): Unit = lock.write {
        if (value == null) {
            if (nullable) {
                cache[name] = null
                node.putNull(name)
            } else {
                throw IllegalArgumentException("Field $name is required and not-nullable, but found null in $this.")
            }
        } else {
            // Clear other type view.
            cache[name] = mutableMapOf(value.javaClass to value)
            node.set(name, mapper.valueToTree(value))
        }
    }

    override fun contains(key: String): Boolean {
        return node.has(key)
    }

    override fun toString(): String = "NodeRaw(${node.properties().joinToString(", ")})"

    override fun contentToString(): String = node.properties().joinToString(", ")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeRaw) return false

        if (node != other.node) return false
        return true
    }

    override fun contentEquals(raw: Raw): Boolean {
        if (raw is NodeRaw) {
            return node == raw.node
        } else if (keys.asSetEquals(raw.keys)) {
            return mapper.valueToTree<ObjectNode>(raw) == node
        } else {
            return false
        }
    }

    override fun hashCode(): Int = node.hashCode()
}

object NodeRawSerializer : StdSerializer<NodeRaw>(NodeRaw::class.java) {
    private fun readResolve(): Any = NodeRawSerializer
    override fun serialize(value: NodeRaw?, generator: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            generator.writeNull()
        } else {
            generator.writeTree(value.node)
        }
    }
}

/**
 * A [Raw] implementation that stores data in a [MutableMap].
 *
 * When accessing fields, it will return the value from the map directly.
 * When modifying fields, it will update the map directly.
 *
 * This class is thread-safe if the [map] is thread-safe.
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
        defaultValueFactory: Supplier<Any?>?
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
                    return defaultValueFactory?.get()
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

    override fun set(
        name: String,
        value: Any?,
        optional: Boolean,
        nullable: Boolean
    ) {
        if (value == null) {
            if (nullable) {
                map[name] = null
            } else {
                throw IllegalArgumentException("Field $name is required and not nullable, but found null in $this.")
            }
        } else {
            map[name] = value
        }
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

/**
 * A [Raw] implementation that stores no data.
 *
 * @author Chuanwise
 */
object EmptyRaw : Raw {
    override fun get(
        name: String,
        type: Type,
        optional: Boolean,
        nullable: Boolean,
        defaultValueFactory: Supplier<Any?>?
    ): Any? {
        if (optional) {
            if (defaultValueFactory != null || nullable) {
                return defaultValueFactory?.get()
            } else {
                throw NoSuchElementException("Field $name is optional, but not found in $this.")
            }
        } else {
            throw IllegalArgumentException("Field $name is required, but not found in $this.")
        }
    }

    override fun set(name: String, value: Any?, optional: Boolean, nullable: Boolean) {
        throw UnsupportedOperationException("Cannot set value to EmptyRaw.")
    }

    override fun contains(key: String): Boolean = false

    override val keys: Iterable<String>
        get() = emptyList()

    override fun contentEquals(raw: Raw): Boolean = raw.isEmpty

    override val isEmpty: Boolean
        get() = true

    override fun contentToString(): String = ""
}