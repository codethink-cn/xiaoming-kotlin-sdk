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

package cn.codethink.xiaoming.serialization

import cn.codethink.xiaoming.util.FIELD_TYPE
import cn.codethink.xiaoming.util.FIELD_VERSION
import cn.codethink.xiaoming.util.Operation
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider

class CodecResolverDsl(
    val resolver: CodecResolver,
    val operation: Operation,
    val replace: Boolean,
    val visible: Boolean
) {
    private class StringCodec<T>(
        private val serializer: (T) -> String,
        private val deserializer: (String) -> T
    ) : Serializer<T>, Deserializer<T> {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
            return deserializer(parser.valueAsString)
        }

        override fun serialize(value: T, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeString(serializer(value))
        }
    }

    private class LongCodec<T>(
        private val serializer: (T) -> Long,
        private val deserializer: (Long) -> T
    ) : Serializer<T>, Deserializer<T> {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
            return deserializer(parser.valueAsLong)
        }

        override fun serialize(value: T, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeNumber(serializer(value))
        }
    }

    private class IntCodec<T>(
        private val serializer: (T) -> Int,
        private val deserializer: (Int) -> T
    ) : Serializer<T>, Deserializer<T> {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
            return deserializer(parser.valueAsInt)
        }

        override fun serialize(value: T, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeNumber(serializer(value))
        }
    }

    inner class Type<T>(
        val type: Class<T>,
        val operation: Operation,
        val replace: Boolean,
        val visible: Boolean
    ) {
        val outer = this@CodecResolverDsl

        inner class Field(
            val nameField: String,
            val operation: Operation,
            val replace: Boolean,
            val visible: Boolean
        ) {
            val outer = this@Type

            inline fun <reified U : T> hint(
                name: String,
                operation: Operation = this.operation,
                replace: Boolean = this.replace,
                visible: Boolean = this.visible,
            ) {
                resolver.registerNameBasedTypeHint(
                    type = type,
                    name = name,
                    nameField = nameField,
                    hint = U::class.java,
                    operation = operation,
                    replace = replace,
                    visible = visible
                )
            }

            inline fun <reified U : T> type(
                name: String,
                operation: Operation = this.operation,
                replace: Boolean = this.replace,
                visible: Boolean = this.visible,
                block: Type<U>.() -> Unit
            ) {
                hint<U>(name, operation, replace, visible)
                type<U>(operation, replace, visible, block)
            }
        }

        inline fun <reified U : T> hint(
            operation: Operation = this.operation,
            replace: Boolean = this.replace
        ) {
            resolver.registerTypeHint(
                type = type,
                hint = U::class.java,
                operation = operation,
                replace = replace
            )
        }

        inline fun field(
            nameField: String,
            operation: Operation = this.operation,
            replace: Boolean = this.replace,
            visible: Boolean = this.visible,
            block: Field.() -> Unit
        ) {
            Field(nameField, operation, replace, visible).block()
        }

        inline fun type(
            operation: Operation = this.operation,
            replace: Boolean = this.replace,
            visible: Boolean = this.visible,
            block: Field.() -> Unit
        ) = field(FIELD_TYPE, operation, replace, visible, block)

        inline fun version(
            operation: Operation = this.operation,
            replace: Boolean = this.replace,
            visible: Boolean = this.visible,
            block: Field.() -> Unit
        ) = field(FIELD_VERSION, operation, replace, visible, block)

        fun string(
            operation: Operation = this.operation,
            replace: Boolean = this.replace,
            deserializer: (String) -> T
        ) {
            string(
                serializer = { it.toString() },
                deserializer = deserializer,
                operation = operation,
                replace = replace
            )
        }

        fun string(
            serializer: (T) -> String,
            deserializer: (String) -> T,
            operation: Operation = this.operation,
            replace: Boolean = this.replace
        ) {
            resolver.registerTokenBasedCodec(
                type = type,
                token = JsonToken.VALUE_STRING,
                codec = StringCodec(
                    serializer = serializer,
                    deserializer = deserializer
                ),
                operation = operation,
                replace = replace
            )
        }

        inline fun <reified U : T> fallback(
            operation: Operation = this.operation,
            replace: Boolean = this.replace
        ) {
            resolver.registerFallbackTypeHint(
                type = type,
                hint = U::class.java,
                operation = operation,
                replace = replace,
            )
        }

        fun long(
            serializer: (T) -> Long,
            deserializer: (Long) -> T,
            operation: Operation = this.operation,
            replace: Boolean = this.replace
        ) {
            resolver.registerTokenBasedCodec(
                type = type,
                token = JsonToken.VALUE_NUMBER_INT,
                codec = LongCodec(
                    serializer = { serializer(it) },
                    deserializer = { deserializer(it) }
                ),
                operation = operation,
                replace = replace
            )
        }

        fun long(
            operation: Operation = this.operation,
            replace: Boolean = this.replace,
            deserializer: (Long) -> T
        ) {
            long(
                serializer = { it.toString().toLong() },
                deserializer = deserializer,
                operation = operation,
                replace = replace
            )
        }

        fun int(
            serializer: (T) -> Int,
            deserializer: (Int) -> T,
            operation: Operation = this.operation,
            replace: Boolean = this.replace
        ) {
            resolver.registerTokenBasedCodec(
                type = type,
                token = JsonToken.VALUE_NUMBER_INT,
                codec = IntCodec(
                    serializer = { serializer(it) },
                    deserializer = { deserializer(it) }
                ),
                operation = operation,
                replace = replace
            )
        }

        fun int(
            operation: Operation = this.operation,
            replace: Boolean = this.replace,
            deserializer: (Int) -> T
        ) {
            int(
                serializer = { it.toString().toInt() },
                deserializer = deserializer,
                operation = operation,
                replace = replace
            )
        }
    }

    inline fun <reified T> type(
        operation: Operation = this.operation,
        replace: Boolean = this.replace,
        visible: Boolean = this.visible,
        block: Type<T>.() -> Unit
    ) {
        Type(T::class.java, operation, replace, visible).block()
    }
}

inline fun CodecResolver.registering(
    operation: Operation,
    replace: Boolean = CodecResolver.DEFAULT_REPLACE,
    visible: Boolean = CodecResolver.DEFAULT_VISIBLE,
    block: CodecResolverDsl.() -> Unit
) {
    CodecResolverDsl(this, operation, replace, visible).block()
}

inline fun CodecResolverInitializeContext.registering(
    operation: Operation = this.operation,
    replace: Boolean = this.replace,
    visible: Boolean = this.visible,
    block: CodecResolverDsl.() -> Unit
) {
    CodecResolverDsl(resolver, operation, replace, visible).block()
}
