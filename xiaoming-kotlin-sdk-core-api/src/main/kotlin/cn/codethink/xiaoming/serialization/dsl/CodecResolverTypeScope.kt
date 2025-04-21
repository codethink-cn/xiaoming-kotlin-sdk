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

package cn.codethink.xiaoming.serialization.dsl

import cn.codethink.xiaoming.serialization.Codec
import cn.codethink.xiaoming.serialization.Deserializer
import cn.codethink.xiaoming.serialization.TypeHint
import cn.codethink.xiaoming.serialization.Serializer
import cn.codethink.xiaoming.util.MutableRegistration
import cn.codethink.xiaoming.util.Operation
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider

class CodecResolverTypeScope<T>(
    val outerScope: CodecResolverScope,
    val rawClass: Class<T>
)

inline fun <reified T> CodecResolverScope.type(block: CodecResolverTypeScope<T>.() -> Unit) {
    CodecResolverTypeScope(this, T::class.java).block()
}

inline fun <reified U : T, T> CodecResolverTypeScope<T>.hint(): MutableRegistration<TypeHint<T, U>>? {
    return outerScope.resolver.registerTypeHint(
        type = rawClass,
        hint = U::class.java,
        operation = outerScope.operation,
        replace = outerScope.replace
    )
}

inline fun <reified T> CodecResolverTypeScope<in T>.fallbackHint(
    replace: Boolean = outerScope.replace,
    operation: Operation = outerScope.operation
): MutableRegistration<TypeHint<in T, T>>? {
    return outerScope.resolver.registerFallbackTypeHint(
        type = rawClass,
        hint = T::class.java,
        operation = operation,
        replace = replace
    )
}

fun <T> CodecResolverTypeScope<T>.token(
    token: JsonToken,
    handler: Codec<T>,
    replace: Boolean = outerScope.replace,
    operation: Operation = outerScope.operation
): MutableRegistration<Codec<T>>? {
    return outerScope.resolver.registerTokenBasedCodec(
        type = rawClass,
        token = token,
        codec = handler,
        operation = operation,
        replace = replace
    )
}

fun <T> CodecResolverTypeScope<T>.string(
    handler: Codec<T>,
    replace: Boolean = outerScope.replace,
    operation: Operation = outerScope.operation
) = token(JsonToken.VALUE_STRING, handler, replace, operation)

fun <T> CodecResolverTypeScope<T>.deserializeFromStringToken(
    replace: Boolean = outerScope.replace,
    operation: Operation = outerScope.operation,
    block: (String) -> T
) = token(JsonToken.VALUE_STRING, handler = createStringTokenDeserializeHandler(block), replace, operation)

private fun <T> createStringTokenDeserializeHandler(block: (String) -> T): Deserializer<T> {
    return Deserializer { parser, _ ->
        val text = parser.text
        block(text)
    }
}

private fun <T> createStringTokenSerializeHandler(block: (T) -> String): Serializer<T> {
    return Serializer { value, generator, _ ->
        val text = block(value)
        generator.writeString(text)
    }
}

private class StringTokenCodec<T>(
    private val serializer: (T) -> String,
    private val deserializer: (String) -> T
) : Serializer<T>, Deserializer<T> {
    override fun serialize(value: T, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(serializer(value))
    }

    override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
        return deserializer(parser.valueAsString)
    }
}

private fun <T> createStringTokenCodecResolver(
    serializer: (T) -> String,
    deserializer: (String) -> T
): Codec<T> {
    return StringTokenCodec(serializer, deserializer)
}

fun <T> CodecResolverTypeScope<T>.string(
    serializer: (T) -> String,
    deserializer: (String) -> T,
    replace: Boolean = outerScope.replace,
    operation: Operation = outerScope.operation
) = token(JsonToken.VALUE_STRING, createStringTokenCodecResolver(serializer, deserializer), replace, operation)
