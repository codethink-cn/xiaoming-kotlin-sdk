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
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * 默认的 [Data] 实现。注意所有 [Data] 的子类都应该继承这个类，并且有一个接受单个 [Raw] 参数的构造函数。
 *
 * @author Chuanwise
 * @see Raw
 * @see Data
 */
@NamingPolicy(policy = DefaultFieldNamingPolicy.SNAKE_CASE)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonSerialize(using = DefaultDataSerializer::class)
abstract class AbstractData(
    final override val raw: Raw
) : Data {
    override fun toString(): String = "${javaClass.simpleName}(${raw.contentToString()})"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractData

        return raw.contentEquals(other.raw)
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

object DefaultDataSerializer : JsonSerializer<Data>() {
    override fun serialize(data: Data, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeObject(data.raw)
    }
}

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