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

package cn.codethink.xiaoming.common

import cn.codethink.xiaoming.io.data.NodeRaw
import cn.codethink.xiaoming.io.data.Raw
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode


/**
 * Common interface of transferring object. All fields stored in a map-like [raw]
 * to allow developer get extension fields.
 *
 * @author Chuanwise
 * @see Raw
 * @see AbstractData
 */
@JsonSerialize(using = DataSerializer::class)
interface Data {
    val raw: Raw
}

object DataSerializer : JsonSerializer<Data>() {
    override fun serialize(data: Data, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeObject(data.raw)
    }
}

/**
 * Default implementation of [Data]. Notice that all subclasses of [Data] should
 * extend this class and have a constructor that accepts a single [Raw] parameter.
 *
 * @author Chuanwise
 * @see Raw
 * @see Data
 */
@InternalApi
abstract class AbstractData(
    override val raw: Raw
) : Data {
    override fun toString(): String = "${javaClass.simpleName}(raw=$raw)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractData

        if (raw != other.raw) return false

        return true
    }

    override fun hashCode(): Int {
        return raw.hashCode()
    }
}

abstract class AbstractDataDeserializer<T : Data>(
    type: Class<T>
) : StdDeserializer<T>(type) {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
        val node = parser.readValueAsTree<ObjectNode>()
        val raw = NodeRaw(parser.codec as ObjectMapper, node)
        return newInstance(raw)
    }

    protected abstract fun newInstance(raw: Raw): T
}

/**
 * A [Data] deserializer that uses reflection to create new instances of [Data].
 * It will construct the object with the constructor that accepts a single [Raw] parameter.
 *
 * @author Chuanwise
 */
class DefaultDataDeserializer<T : Data>(
    private val type: Class<T>
) : AbstractDataDeserializer<T>(type) {
    companion object {
        @JvmStatic
        private val parameterTypes: Array<Class<*>> = arrayOf(Raw::class.java)
    }

    override fun newInstance(raw: Raw): T {
        return getOrConstruct(type, parameterTypes, arrayOf(raw))
    }
}

/**
 * Kotlin-friendly version of constructor of [DefaultDataDeserializer].
 */
inline fun <reified T : Data> DefaultDataDeserializer() = DefaultDataDeserializer(T::class.java)
