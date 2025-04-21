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
import java.util.NoSuchElementException
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Supplier
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 通过 Jackson 表示树形结构的 [ObjectNode] 存储原始数据的 [Store]。
 *
 * @author Chuanwise
 */
@JsonSerialize(using = NodeRawSerializer::class)
class NodeStoreImpl(
    private val mapper: ObjectMapper,
    private val node: ObjectNode
) : AbstractStore(), MutableStore {
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    // Key: field name, Value: null (if value is set to null) or a map of type to value.
    private val cache: MutableMap<String, MutableMap<Type, Any?>?> = mutableMapOf()
    override val keys: Set<String> get() = node.fieldNames().toSet()
    override val size: Int get() = node.size()

    override val isEmpty: Boolean get() = node.isEmpty

    internal fun toNode(): ObjectNode = node

    @Suppress("UNCHECKED_CAST")
    private fun <T> getValueNoCache(name: String, meta: TypeMeta<T>, defaultValueFactory: Supplier<T>?, throwException: Boolean): T? {
        val node = node[name]
        if (node == null) {
            if (defaultValueFactory == null) {
                if (throwException) {
                    throw NoSuchElementException("Field $name is required, but not found in $this.")
                }
                return null
            }
            return defaultValueFactory.get()
        }
        if (node is NullNode) {
            return if (meta.nullable) {
                null
            } else if (throwException) {
                throw NullPointerException("Field $name is required and not-nullable, but found null in $this.")
            } else {
                null
            }
        }

        val type = meta.type
        when (type) {
            Store::class.java -> {
                require(node is ObjectNode) { "Field $name is not a Raw, but a ${node.javaClass}." }
                return NodeStoreImpl(mapper, node) as T
            }

            JsonNode::class.java -> return node as T
            JsonToken::class.java -> return node.asToken() as T
        }

        val jacksonType = mapper.constructType(type)
        val value: Any? = mapper.readValue(node.traverse(mapper), jacksonType)
        if (value == null && !meta.nullable) {
            if (throwException) {
                throw NullPointerException("Field $name is not-nullable, but found null in $this.")
            }
            return null
        }

        return value as T?
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> get(name: String, meta: TypeMeta<T>, defaultValueFactory: Supplier<T>?, throwException: Boolean): T? {
        val valueNoCache: T
        var values: MutableMap<Type, Any?>?

        lock.read {
            values = cache[name]

            val valuesCached = cache.containsKey(name)
            val valuesLocal = values

            if (valuesCached) {
                if (valuesLocal == null) {
                    if (!meta.nullable) {
                        if (throwException) {
                            throw NullPointerException("Field $name is not-nullable, but found null in $this.")
                        }
                        return null
                    }
                    return null as T
                }

                val typeValue = valuesLocal[meta.type]
                val typeValueCached = valuesLocal.containsKey(meta.type)

                if (typeValueCached) {
                    return typeValue as T
                }
            }

            valueNoCache = getValueNoCache(name, meta, defaultValueFactory, throwException = throwException) as T
        }

        lock.write {
            val valuesLocal = values
            if (valuesLocal == null) {
                values = mutableMapOf(meta.type to valueNoCache)
                cache[name] = values
            } else {
                valuesLocal[meta.type] = valueNoCache
            }
        }

        return valueNoCache
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(name: String, meta: TypeMeta<T>, defaultValueFactory: Supplier<T>?): T =
        get(name, meta, defaultValueFactory, throwException = true) as T

    override fun <T> getOrNull(name: String, meta: TypeMeta<T>, defaultValueFactory: Supplier<T>?): T? =
        get(name, meta, defaultValueFactory, throwException = false)

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

    override fun remove(name: String) {
        lock.write {
            cache.remove(name)
            node.remove(name)
        }
    }

    override fun clear() {
        lock.write {
            cache.clear()
            node.removeAll()
        }
    }

    override fun contentToString(): String = node.properties().joinToString(", ")
}

object NodeRawSerializer : StdSerializer<NodeStoreImpl>(NodeStoreImpl::class.java) {
    private fun readResolve(): Any = NodeRawSerializer
    override fun serialize(value: NodeStoreImpl?, generator: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            generator.writeNull()
        } else {
            generator.writeTree(value.toNode())
        }
    }
}