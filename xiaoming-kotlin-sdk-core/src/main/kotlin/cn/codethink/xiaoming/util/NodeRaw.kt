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
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.lang.reflect.Type
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 通过 Jackson 表示树形结构的 [ObjectNode] 存储原始数据的 [Raw]。
 *
 * @author Chuanwise
 */
@JsonSerialize(using = NodeRawSerializer::class)
class NodeRaw(
    private val mapper: ObjectMapper,
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
        convertable: Boolean,
        defaultValueFactory: (() -> Any?)?
    ): Any? = lock.read {
        // Method to read value from node.
        fun doGetValue(): Any? {
            val fieldNode = node[name]
                ?: if (optional) {
                    if (defaultValueFactory != null || nullable) {
                        return defaultValueFactory?.invoke()
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

            if (type === Raw::class.java) {
                require(fieldNode is ObjectNode) { "Field $name is not a Raw, but a ${fieldNode.javaClass}." }
                return NodeRaw(mapper, fieldNode)
            } else if (type === JsonNode::class.java) {
                return fieldNode
            } else if (type === JsonToken::class.java) {
                return fieldNode.asToken()
            }

            if (convertable) {
                if (type === Boolean::class.java) {
                    return fieldNode.asBoolean()
                } else if (type === Double::class.java) {
                    return fieldNode.asDouble()
                } else if (type === Int::class.java) {
                    return fieldNode.asInt()
                } else if (type === Long::class.java) {
                    return fieldNode.asLong()
                } else if (type === String::class.java) {
                    return fieldNode.asText()
                }
            }

            val javaType = mapper.constructType(type)
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

    override operator fun set(name: String, value: Any?): Unit = lock.write {
        if (value == null) {
            cache[name] = null
            node.putNull(name)
        } else {
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
        return if (raw is NodeRaw) {
            node == raw.node
        } else if (keys.asSetEquals(raw.keys)) {
            mapper.valueToTree<ObjectNode>(raw) == node
        } else {
            false
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