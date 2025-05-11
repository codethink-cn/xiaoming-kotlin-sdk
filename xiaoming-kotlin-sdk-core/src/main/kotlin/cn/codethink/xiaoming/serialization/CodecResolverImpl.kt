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

import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Jacksons
import cn.codethink.xiaoming.util.MutableRegistration
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.compareAndRemove
import cn.codethink.xiaoming.util.inheritedClasses
import cn.codethink.xiaoming.util.putIfAbsentOrReplace
import cn.codethink.xiaoming.util.withValue
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.type.ArrayType
import com.fasterxml.jackson.databind.type.CollectionLikeType
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.MapLikeType
import com.fasterxml.jackson.databind.type.MapType
import com.fasterxml.jackson.databind.type.ReferenceType
import com.fasterxml.jackson.databind.util.TokenBuffer
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

@OptIn(InternalApi::class)
@Suppress("UNCHECKED_CAST")
class CodecResolverImpl : CodecResolver {
    private data class TypeHintImpl<F, T : F>(
        override val type: Class<F>,
        override val hint: Class<T>
    ) : TypeHint<F, T>

    private class AsJacksonSerializer<T>(type: Class<T>, private val serializer: Serializer<T>) : StdSerializer<T>(type) {
        override fun serialize(value: T, generator: JsonGenerator, provider: SerializerProvider) {
            serializer.serialize(value, generator, provider)
        }
    }

    private class AsJacksonDeserializer<T>(type: Class<T>, private val deserializer: Deserializer<T>) : StdDeserializer<T>(type) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): T = deserializer.deserialize(parser, context)
    }

    private abstract class AbstractCodecEntry<T>(val type: Class<T>, val lock: ReentrantReadWriteLock) {
        var serializer: AbstractCodecRegistration? = null
        var deserializer: AbstractCodecRegistration? = null

        abstract inner class AbstractCodecRegistration(
            open val value: Codec<T>
        ) {
            val type: Class<T> = this@AbstractCodecEntry.type

            open val asJacksonSerializer: AsJacksonSerializer<T>? = null
            open val asJacksonDeserializer: AsJacksonDeserializer<T>? = null

            val asJacksonSerializerOrFail: AsJacksonSerializer<T> get() = asJacksonSerializer ?: error("Not a serializer")
            val asJacksonDeserializerOrFail: AsJacksonDeserializer<T> get() = asJacksonDeserializer ?: error("Not a deserializer")

            abstract fun tryRemoveFromEntry(): Boolean
        }

        open inner class SerializerRegistration(
            final override val value: Serializer<T>,
            final override val operation: Operation
        ) : AbstractCodecRegistration(value), MutableRegistration<Serializer<T>> {
            override val isRemoved: Boolean get() = lock.read { serializer !== this }
            override val asJacksonSerializer = AsJacksonSerializer(type, value)

            override fun tryRemoveFromEntry(): Boolean = lock.write {
                val current = serializer
                if (current === this) {
                    serializer = null
                    return true
                }
                return false
            }

            override fun ensureRemoved(): Boolean {
                val result = tryRemoveFromEntry()
                if (result && isEmpty()) {
                    removeEntry()
                }
                return result
            }

            override fun remove() {
                check(ensureRemoved()) { "Failed to remove serialize codec registration for $value" }
            }
        }

        open inner class DeserializerRegistration(
            final override val value: Deserializer<T>,
            final override val operation: Operation
        ) : AbstractCodecRegistration(value), MutableRegistration<Deserializer<T>> {
            override val isRemoved: Boolean get() = deserializer !== this
            override val asJacksonDeserializer = AsJacksonDeserializer(type, value)

            override fun tryRemoveFromEntry(): Boolean = lock.write {
                val current = deserializer
                if (current === this) {
                    deserializer = null
                    return true
                }
                return false
            }

            override fun ensureRemoved(): Boolean {
                val result = tryRemoveFromEntry()
                if (result && isEmpty()) {
                    removeEntry()
                }
                return result
            }

            override fun remove() {
                check(ensureRemoved()) { "Failed to remove deserialize codec registration for $value" }
            }
        }

        open inner class CodecRegistration(
            final override val value: Codec<T>,
            final override val operation: Operation
        ) : AbstractCodecRegistration(value), MutableRegistration<Codec<T>> {
            override val isRemoved: Boolean get() = lock.read { serializer !== this }

            override val asJacksonSerializer = AsJacksonSerializer(type, value as Serializer<T>)
            override val asJacksonDeserializer = AsJacksonDeserializer(type, value as Deserializer<T>)

            override fun tryRemoveFromEntry(): Boolean {
                lock.write {
                    var result = false

                    val currentSerializer = serializer
                    if (currentSerializer === this) {
                        serializer = null
                        result = true
                    }

                    val currentDeserializer = deserializer
                    if (currentDeserializer === this) {
                        deserializer = null
                        result = true
                    }
                    return result
                }
            }

            override fun ensureRemoved(): Boolean {
                val result = tryRemoveFromEntry()
                if (result && isEmpty()) {
                    removeEntry()
                }
                return result
            }

            override fun remove() {
                check(ensureRemoved()) { "Failed to remove codec registration for $value" }
            }
        }

        private fun isEmpty(): Boolean {
            return serializer == null && deserializer == null
        }

        protected fun throwUnexpectedCodecException(): Nothing {
            throw IllegalArgumentException("Codec must be either a Serializer or a Deserializer")
        }

        abstract fun removeEntry()

        protected fun registerToEntry(registration: AbstractCodecRegistration, replace: Boolean): Boolean {
            var result = false

            val isSerializer = registration.value is Serializer<T>
            val isDeserializer = registration.value is Deserializer<T>

            lock.write {
                // 如果以前注册的兼具序列化和反序列化功能，则将二者都移除。
                if (serializer != null && replace && serializer === deserializer) {
                    serializer = null
                    deserializer = null
                }

                if (isSerializer && (serializer == null || replace)) {
                    serializer = registration
                    result = true
                }
                if (isDeserializer && (deserializer == null || replace)) {
                    deserializer = registration
                    result = true
                }
            }

            return result
        }

        protected open fun registerCodec(
            codec: Codec<T>,
            operation: Operation,
            replace: Boolean
        ): MutableRegistration<Codec<T>>? {
            val isSerializer = codec is Serializer<*>
            val isDeserializer = codec is Deserializer<*>

            val registration = when {
                isSerializer && isDeserializer -> CodecRegistration(codec, operation)
                isSerializer -> SerializerRegistration(codec as Serializer<T>, operation)
                isDeserializer -> DeserializerRegistration(codec as Deserializer<T>, operation)
                else -> throwUnexpectedCodecException()
            }

            return registration.takeIf { registerToEntry(registration, replace) }
        }
    }

    private class TypeHints {
        // type -> hint
        private val hints = ConcurrentHashMap<Class<*>, MutableRegistration<TypeHint<*, *>>>()

        private inner class MutableHintRegistrationImpl<V : TypeHint<*, *>>(
            override val value: V,
            override val operation: Operation
        ) : MutableRegistration<V> {
            override val isRemoved: Boolean get() = hints[value.type] === this

            override fun remove() {
                check(ensureRemoved()) { "Failed to remove type hint registration for ${value.type} -> ${value.hint}" }
            }

            override fun ensureRemoved(): Boolean {
                return hints.compareAndRemove(value.type, this)
            }
        }

        fun <F, T : F> registerTypeHint(
            type: Class<F>,
            hint: Class<T>,
            operation: Operation,
            replace: Boolean
        ): MutableRegistration<TypeHint<F, T>>? {
            val registration = MutableHintRegistrationImpl(TypeHintImpl(type, hint), operation)
            return hints.putIfAbsentOrReplace(type, replace, registration) as MutableRegistration<TypeHint<F, T>>?
        }

        fun <T> findTypeHint(type: Class<T>): MutableRegistration<TypeHint<T, out T>>? {
            return hints[type] as MutableRegistration<TypeHint<T, out T>>?
        }
    }

    private val typeHints = TypeHints()

    private val fallbackTypeHints = TypeHints()

    private class NameClue(
        val nameField: String,
        val name: String,
        val visible: Boolean
    )

    private class NameBasedCodecs {
        private val lock = ReentrantReadWriteLock()

        // type -> nameField -> name -> codec
        private val codecs = HashMap<Class<*>, HashMap<String, HashMap<String, CodecEntry<*>>>>()

        inner class CodecEntry<T>(type: Class<T>, val nameField: String, val name: String) : AbstractCodecEntry<T>(type, lock) {
            private val nameClueVisibleTrue = NameClue(nameField, name, true)
            private val nameClueVisibleFalse = NameClue(nameField, name, false)

            private fun NameClue(visible: Boolean): NameClue {
                return if (visible) nameClueVisibleTrue else nameClueVisibleFalse
            }

            inner class CodecRegistrationImpl(
                value: Codec<T>,
                operation: Operation,
                visible: Boolean
            ) : CodecRegistration(value, operation) {
                val nameClue = NameClue(visible)
            }

            inner class SerializerRegistrationImpl(
                value: Serializer<T>,
                operation: Operation,
                visible: Boolean
            ) : SerializerRegistration(value, operation) {
                val nameClue = NameClue(visible)
            }

            inner class DeserializerRegistrationImpl(
                value: Deserializer<T>,
                operation: Operation,
                visible: Boolean
            ) : DeserializerRegistration(value, operation) {
                val nameClue = NameClue(visible)
            }

            fun registerCodec(
                codec: Codec<T>,
                visible: Boolean,
                operation: Operation,
                replace: Boolean
            ): MutableRegistration<Codec<T>>? {
                val isSerializer = codec is Serializer<*>
                val isDeserializer = codec is Deserializer<*>

                val registration = when {
                    isSerializer && isDeserializer -> CodecRegistrationImpl(codec, operation, visible)
                    isSerializer -> SerializerRegistrationImpl(codec as Serializer<T>, operation, visible)
                    isDeserializer -> DeserializerRegistrationImpl(codec as Deserializer<T>, operation, visible)
                    else -> throwUnexpectedCodecException()
                }

                return registration.takeIf { registerToEntry(registration, replace) }
            }

            override fun removeEntry() {
                lock.write {
                    val typeCodecs = codecs[type] ?: return
                    val nameFieldCodecs = typeCodecs[nameField] ?: return

                    nameFieldCodecs.remove(name)

                    if (nameFieldCodecs.isEmpty()) {
                        typeCodecs.remove(nameField)
                        if (typeCodecs.isEmpty()) {
                            codecs.remove(type)
                        }
                    }
                }
            }
        }

        fun <T> registerNameBasedCodec(
            type: Class<T>,
            nameField: String,
            name: String,
            codec: Codec<T>,
            visible: Boolean,
            operation: Operation,
            replace: Boolean
        ): MutableRegistration<Codec<T>>? {
            return lock.write {
                val entry = codecs.computeIfAbsent(type) { HashMap() }
                    .computeIfAbsent(nameField) { HashMap() }
                    .computeIfAbsent(name) { CodecEntry(type, nameField, name) } as CodecEntry<T>

                entry.registerCodec(codec, visible, operation, replace)
            }
        }

        fun <T> findNameBasedEntries(
            type: Class<T>
        ): Map<String, Map<String, CodecEntry<T>>>? {
            return lock.read { codecs[type] } as Map<String, Map<String, CodecEntry<T>>>?
        }
    }

    private val nameBasedCodecs = NameBasedCodecs()

    private class NameBasedTypeHints {
        private val lock = ReentrantReadWriteLock()

        // type -> nameField -> name -> hint
        private val hints = HashMap<Class<*>, HashMap<String, HashMap<String, MutableHintRegistration<*, *>>>>()

        inner class MutableHintRegistration<F, T : F>(
            override val value: TypeHint<F, T>,
            override val operation: Operation,
            val nameClue: NameClue
        ) : MutableRegistration<TypeHint<F, T>> {
            override val isRemoved: Boolean get() = lock.read { hints[value.type]?.get(nameClue.nameField)?.get(nameClue.name) !== this }

            override fun ensureRemoved(): Boolean {
                lock.write {
                    val current = hints[value.type]?.get(nameClue.nameField)?.get(nameClue.name)
                    if (current === this) {
                        hints[value.type]?.get(nameClue.nameField)?.remove(nameClue.name)
                        return true
                    }
                    return false
                }
            }

            override fun remove() {
                check(ensureRemoved()) { "Failed to remove name-based hint registration for ${value.type} -> ${nameClue.nameField} -> ${nameClue.name}" }
            }
        }

        fun <F, T : F> registerNameBasedTypeHint(
            type: Class<F>,
            nameField: String,
            name: String,
            hint: Class<T>,
            visible: Boolean,
            operation: Operation,
            replace: Boolean
        ): MutableHintRegistration<F, T>? {
            val registration = MutableHintRegistration(
                TypeHintImpl(type, hint),
                operation,
                NameClue(nameField, name, visible)
            )

            lock.write {
                val typeHints = hints.computeIfAbsent(type) { HashMap() }
                val nameFieldHints = typeHints.computeIfAbsent(nameField) { HashMap() }
                return nameFieldHints.putIfAbsentOrReplace(name, replace, registration) as MutableHintRegistration<F, T>?
            }
        }

        fun <T> findNameBasedTypeHints(
            type: Class<T>
        ): Map<String, Map<String, MutableHintRegistration<T, out T>>>? {
            return lock.read { hints[type] } as Map<String, Map<String, MutableHintRegistration<T, out T>>>?
        }
    }

    private val nameBasedTypeHints = NameBasedTypeHints()

    private class TokenBasedCodecs {
        private val lock = ReentrantReadWriteLock()

        private val codecs = HashMap<Class<*>, EnumMap<JsonToken, CodecEntry<*>>>()

        inner class CodecEntry<T>(type: Class<T>, val token: JsonToken) : AbstractCodecEntry<T>(type, lock) {
            override fun removeEntry() {
                lock.write {
                    val typeCodecs = codecs[type] ?: return
                    typeCodecs.remove(token)
                    if (typeCodecs.isEmpty()) {
                        codecs.remove(type)
                    }
                }
            }

            public override fun registerCodec(
                codec: Codec<T>,
                operation: Operation,
                replace: Boolean
            ): MutableRegistration<Codec<T>>? {
                return super.registerCodec(codec, operation, replace)
            }
        }

        fun <T> registerTokenBasedCodec(
            type: Class<T>,
            token: JsonToken,
            codec: Codec<T>,
            operation: Operation,
            replace: Boolean
        ): MutableRegistration<Codec<T>>? {
            return lock.write {
                val entry = codecs.computeIfAbsent(type) { EnumMap(JsonToken::class.java) }
                    .computeIfAbsent(token) { CodecEntry(type, token) } as CodecEntry<T>

                entry.registerCodec(codec, operation, replace)
            }
        }

        fun <T> findTokenBasedSerializer(type: Class<T>, token: JsonToken): AbstractCodecEntry<T>.AbstractCodecRegistration? {
            return lock.read { codecs[type]?.get(token)?.serializer } as AbstractCodecEntry<T>.AbstractCodecRegistration?
        }

        fun <T> findTokenBasedDeserializer(type: Class<T>, token: JsonToken): AbstractCodecEntry<T>.AbstractCodecRegistration? {
            return lock.read { codecs[type]?.get(token)?.deserializer } as AbstractCodecEntry<T>.AbstractCodecRegistration?
        }

        fun <T> findTokenBasedCodecs(type: Class<T>): EnumMap<JsonToken, CodecEntry<T>>? {
            return lock.read { codecs[type] } as EnumMap<JsonToken, CodecEntry<T>>?
        }
    }

    private val tokenBasedCodecs = TokenBasedCodecs()

    private class TypeBasedCodecs {
        private val lock = ReentrantReadWriteLock()

        // type -> codec
        private val codecs = HashMap<Class<*>, CodecEntry<*>>()

        inner class CodecEntry<T>(type: Class<T>) : AbstractCodecEntry<T>(type, lock) {
            override fun removeEntry() {
                lock.write {
                    codecs.remove(type)
                }
            }

            public override fun registerCodec(
                codec: Codec<T>,
                operation: Operation,
                replace: Boolean
            ): MutableRegistration<Codec<T>>? {
                return super.registerCodec(codec, operation, replace)
            }
        }

        fun <T> registerTypeBasedCodec(
            type: Class<T>,
            codec: Codec<T>,
            operation: Operation,
            replace: Boolean
        ): MutableRegistration<Codec<T>>? {
            return lock.write {
                val entry = codecs.computeIfAbsent(type) { CodecEntry(type) } as CodecEntry<T>
                entry.registerCodec(codec, operation, replace)
            }
        }

        fun <T> findSerializer(type: Class<T>): AbstractCodecEntry<T>.AbstractCodecRegistration? {
            return lock.read { codecs[type]?.serializer } as AbstractCodecEntry<T>.AbstractCodecRegistration?
        }

        fun <T> findDeserializer(type: Class<T>): AbstractCodecEntry<T>.AbstractCodecRegistration? {
            return lock.read { codecs[type]?.deserializer } as AbstractCodecEntry<T>.AbstractCodecRegistration?
        }
    }

    private val typeBasedCodecs = TypeBasedCodecs()

    // 用于作废缓存的类型序列化处理器等内容。
    // 每次本模块提供给 Jackson 的序列化器或反序列化器动态查找反序列化器时，都会先检查版本号。
    // 每次查找的结果会和版本号一起存储，以便一次性作废。
    // @see VersionCache
    private val version = AtomicInteger(0)

//    private class SerializerCache

    override fun <F, T : F> registerTypeHint(
        type: Class<F>,
        hint: Class<T>,
        operation: Operation,
        replace: Boolean
    ): MutableRegistration<TypeHint<F, T>>? {
        invalidateCache()
        return typeHints.registerTypeHint(type, hint, operation, replace)
    }

    override fun <F, T : F> registerFallbackTypeHint(
        type: Class<F>,
        hint: Class<T>,
        operation: Operation,
        replace: Boolean
    ): MutableRegistration<TypeHint<F, T>>? {
        invalidateCache()
        return fallbackTypeHints.registerTypeHint(type, hint, operation, replace)
    }

    override fun <T> registerNameBasedCodec(
        type: Class<T>,
        nameField: String,
        name: String,
        codec: Codec<T>,
        operation: Operation,
        visible: Boolean,
        replace: Boolean
    ): MutableRegistration<Codec<T>>? {
        invalidateCache()
        return nameBasedCodecs.registerNameBasedCodec(type, nameField, name, codec, visible, operation, replace)
    }

    override fun <F, T : F> registerNameBasedTypeHint(
        type: Class<F>,
        nameField: String,
        name: String,
        hint: Class<T>,
        operation: Operation,
        visible: Boolean,
        replace: Boolean
    ): MutableRegistration<TypeHint<F, T>>? {
        invalidateCache()
        return nameBasedTypeHints.registerNameBasedTypeHint(type, nameField, name, hint, visible, operation, replace)
    }

    override fun <T> registerTokenBasedCodec(
        type: Class<T>,
        token: JsonToken,
        codec: Codec<T>,
        operation: Operation,
        replace: Boolean
    ): MutableRegistration<Codec<T>>? {
        invalidateCache()
        return tokenBasedCodecs.registerTokenBasedCodec(type, token, codec, operation, replace)
    }

    override fun <T> registerTypeBasedCodec(
        type: Class<T>,
        codec: Codec<T>,
        operation: Operation,
        replace: Boolean
    ): MutableRegistration<Codec<T>>? {
        invalidateCache()
        return typeBasedCodecs.registerTypeBasedCodec(type, codec, operation, replace)
    }

    private data class ClassKey<U, R : U>(val type: Class<U>, val hintedType: Class<R>)

    private inner class NameBasedTypeHintClueCollector<T>(val type: Class<T>, val hintedType: Class<out T>) {
        var currentType: Class<out T> = type

        fun collect(): List<NameBasedTypeHints.MutableHintRegistration<out T, out T>> {
            var currentTypeHints = nameBasedTypeHints.findNameBasedTypeHints(currentType)
            val entries = mutableListOf<NameBasedTypeHints.MutableHintRegistration<out T, out T>>()

            while (currentTypeHints != null) {
                var entry: NameBasedTypeHints.MutableHintRegistration<out T, out T>? = null
                for ((nameField, nameFieldEntries) in currentTypeHints) {
                    for ((currentName, currentEntry) in nameFieldEntries) {
                        if (!currentEntry.value.hint.isAssignableFrom(hintedType)) {
                            continue
                        }

                        check(entry == null) {
                            "Ambiguous name-based type hints for $type: $nameField -> $currentName vs ${currentEntry.nameClue.name}"
                        }
                        entry = currentEntry
                    }
                }
                if (entry == null) {
                    break
                }

                entries.add(entry)

                currentType = entry.value.hint
                currentTypeHints = nameBasedTypeHints.findNameBasedTypeHints(currentType)
            }

            return entries
        }
    }

    private object SerializerProviderMethodCaller {
        // protected JsonSerializer<Object> _createUntypedSerializer(JavaType type)
        private val lookup = MethodHandles.privateLookupIn(SerializerProvider::class.java, MethodHandles.lookup())
        private val methodHandle = lookup.findVirtual(
            SerializerProvider::class.java,
            "_createUntypedSerializer",
            MethodType.methodType(JsonSerializer::class.java, JavaType::class.java)
        )

        fun createUntypedSerializer(provider: SerializerProvider, type: JavaType): JsonSerializer<*> {
            return methodHandle.invoke(provider, type) as JsonSerializer<*>
        }
    }

    private inner class JacksonSerializers : Serializers {
        // 因为本模块并不提供最终的序列化实现，它返回的 Serializer 只是内置了动态调度功能，
        // 最终的序列化仍然依靠其他模块实现（即，“如果没有安装本模块”的序列化器）。
        // 然而 Jackson SerializerProvider 里的 findSerializer 都会查 cache，并命中本模块的调度用序列化器。

        // 根据 Jackson 源代码，其最终调用的是 _createUntypedSerializer，内部使用的是 _serializerFactory 创建序列化器，而且不会缓存。
        // 而 _serializerFactory 创建序列化器时会先查询各 module 是否有序列化器，即调用下面的 findSerializer 方法。故需要使用此 ThreadLocal 控制行为。
        private val disableSerializerFindFunctions = ThreadLocal<Unit?>()

        private fun <T> JsonSerializer<T>.replaceIfContextual(provider: SerializerProvider, property: BeanProperty?): JsonSerializer<T> {
            return if (this is ContextualSerializer) {
                createContextual(provider, property) as JsonSerializer<T>
            } else {
                this
            }
        }

        private fun <T> findContextualBeanSerializer(type: Class<T>, property: BeanProperty?, provider: SerializerProvider): JsonSerializer<T> {
            val serializer = disableSerializerFindFunctions.withValue {
                SerializerProviderMethodCaller.createUntypedSerializer(provider, provider.constructType(type))
            }
            return serializer.replaceIfContextual(provider, property) as JsonSerializer<T>
        }

        private inner class SerializerResolver<T : Any>(
            val type: Class<T>,
            val hintedType: Class<out T>,
            val provider: SerializerProvider,
            val property: BeanProperty?,
            val operations: MutableList<Operation> = mutableListOf()
        ) {
            fun resolve(): JsonSerializer<out T>? {
                resolveByTypeHints(typeHints)?.let { return it }
                typeBasedCodecs.findSerializer(type)?.let { return it.asJacksonSerializer }

                tokenBasedCodecs.findTokenBasedCodecs(type)?.let { entries ->
                    entries.mapValues { it.value.serializer }
                        .filterValues { it != null }
                        .values
                        .singleOrNull()
                        ?.let { return it.asJacksonSerializer }
                }

                nameBasedCodecs.findNameBasedEntries(type)?.let { entries ->
                    entries.flatMap { it.value.values }
                        .mapNotNull { it.serializer }
                        .singleOrNull()
                        ?.let { return it.asJacksonSerializer }
                }

                val nameBasedTypeHintClueCollector = NameBasedTypeHintClueCollector(type, hintedType)
                val nameBasedTypeHintRegistrations = nameBasedTypeHintClueCollector.collect()
                if (nameBasedTypeHintRegistrations.isNotEmpty()) {
                    return HintedJacksonSerializer(nameBasedTypeHintClueCollector.currentType as Class<T>, nameBasedTypeHintRegistrations, property)
                }

                return resolveByTypeHints(fallbackTypeHints)
            }

            private fun resolveByTypeHints(typeHints: TypeHints): JsonSerializer<T>? {
                return typeHints.findTypeHint(type)?.let {
                    findContextualBeanSerializer(it.value.hint, property, provider) as JsonSerializer<T>?
                }
            }
        }

        private inner class DynamicJacksonSerializer<T : Any>(private val type: Class<T>) : StdSerializer<T>(type), ContextualSerializer {
            private val serializersCache = VersionCache<MutableMap<ClassKey<*, *>, JsonSerializer<*>>>()

            override fun serialize(value: T, generator: JsonGenerator, provider: SerializerProvider) {
                serializeContextually(value, generator, provider, property = null)
            }

            private inner class ContextualDynamicJsonSerializer(private val property: BeanProperty?) : StdSerializer<T>(type) {
                override fun serialize(value: T, generator: JsonGenerator, provider: SerializerProvider) {
                    serializeContextually(value, generator, provider, property)
                }
            }

            override fun createContextual(provider: SerializerProvider, property: BeanProperty?): JsonSerializer<*> {
                return ContextualDynamicJsonSerializer(property)
            }

            private fun serializeContextually(
                value: T,
                generator: JsonGenerator,
                provider: SerializerProvider,
                property: BeanProperty?
            ) {
                val type = property?.type?.rawClass as Class<T>? ?: type
                val hintedType = value::class.java
                val key = ClassKey(type, hintedType)

                val serializer = serializersCache
                    .compute { ConcurrentHashMap() }
                    .computeIfAbsent(key) {
                        SerializerResolver(
                            type = type,
                            hintedType = hintedType,
                            provider = provider,
                            property = property
                        ).resolve() ?: findContextualBeanSerializer(type, property, provider)
                    } as JsonSerializer<T>

                serializer.serialize(value, generator, provider)
            }
        }

        private inner class HintedJacksonSerializer<T>(
            private val type: Class<T>,
//            private val hintedType: Class<out T>,
            private val typeHintRegistrations: List<NameBasedTypeHints.MutableHintRegistration<out T, out T>>,
            private val property: BeanProperty?
        ) : StdSerializer<T>(type) {
            private val serializerCache = VersionCache<JsonSerializer<T>>()

            override fun serialize(value: T, generator: JsonGenerator, provider: SerializerProvider) {
                val buffer = TokenBuffer(generator.codec, false)
                val serializer = serializerCache.compute {
                    findContextualBeanSerializer(type, property, provider)
                }

                serializer.serialize(value, buffer, provider)

                val node = buffer.asParser().readValueAsTree<JsonNode>()
                if (node is ObjectNode) {
                    for (registration in typeHintRegistrations) {
                        val nameClue = registration.nameClue

                        check(!node.has(nameClue.nameField)) {
                            val conflicted = node[nameClue.nameField]
                            "Conflict name field for $type: ${nameClue.nameField} -> $conflicted (from serialized) " +
                                    "vs ${nameClue.name} (from clues due to ${registration.operation.description})"
                        }

                        node.put(nameClue.nameField, nameClue.name)
                    }
                }

                generator.writeTree(node)
            }
        }

        private fun <T> getSerializerClassWeak(type: Class<T>): Class<in T>? {
            return type.inheritedClasses
                .firstNotNullOfOrNull {
                    getSerializerClassStrong(it)
                } as Class<in T>?
        }

        private fun <T> getSerializerClassStrong(type: Class<T>): Class<in T>? {
            typeHints.findTypeHint(type)?.let { return type }
            typeBasedCodecs.findSerializer(type)?.let { return type }

            tokenBasedCodecs.findTokenBasedCodecs(type)?.let { entries ->
                entries.mapValues { it.value.serializer }
                    .filterValues { it != null }
                    .values
                    .singleOrNull()
                    ?.let { return type }
            }

            nameBasedCodecs.findNameBasedEntries(type)?.let { entries ->
                entries.flatMap { it.value.values }
                    .mapNotNull { it.serializer }
                    .singleOrNull()
                    ?.let { return it.type }
            }

            nameBasedTypeHints.findNameBasedTypeHints(type)?.let { entries ->
                entries.flatMap { it.value.values }
                    .map { it.value.hint }
                    .singleOrNull()
                    ?.let { return type }
            }

            fallbackTypeHints.findTypeHint(type)?.let { return type }
            return null
        }

        val dynamicSerializers = ConcurrentHashMap<Class<*>, DynamicJacksonSerializer<*>>()

        override fun findSerializer(config: SerializationConfig, type: JavaType, beanDesc: BeanDescription): JsonSerializer<*>? {
            return findSerializer(type.rawClass)
        }

        fun <T> findSerializer(type: Class<T>): JsonSerializer<T>? {
            if (disableSerializerFindFunctions.get() != null) {
                return null
            }

            dynamicSerializers[type]?.let { return it as JsonSerializer<T> }

            getSerializerClassWeak(type)?.let {
                val parentTypeSerializer = dynamicSerializers.computeIfAbsent(it) { _ -> DynamicJacksonSerializer(it) }
                dynamicSerializers[type] = parentTypeSerializer
                return parentTypeSerializer as JsonSerializer<T>
            }

            return null
        }

        override fun findReferenceSerializer(
            config: SerializationConfig?,
            type: ReferenceType?,
            beanDesc: BeanDescription?,
            contentTypeSerializer: TypeSerializer?,
            contentValueSerializer: JsonSerializer<Any>?
        ): JsonSerializer<*>? = null

        override fun findArraySerializer(
            config: SerializationConfig?,
            type: ArrayType?,
            beanDesc: BeanDescription?,
            elementTypeSerializer: TypeSerializer?,
            elementValueSerializer: JsonSerializer<Any>?
        ): JsonSerializer<*>? = null

        override fun findCollectionSerializer(
            config: SerializationConfig?,
            type: CollectionType?,
            beanDesc: BeanDescription?,
            elementTypeSerializer: TypeSerializer?,
            elementValueSerializer: JsonSerializer<Any>?
        ): JsonSerializer<*>? = null

        override fun findCollectionLikeSerializer(
            config: SerializationConfig?,
            type: CollectionLikeType?,
            beanDesc: BeanDescription?,
            elementTypeSerializer: TypeSerializer?,
            elementValueSerializer: JsonSerializer<Any>?
        ): JsonSerializer<*>? = null

        override fun findMapSerializer(
            config: SerializationConfig?,
            type: MapType?,
            beanDesc: BeanDescription?,
            keySerializer: JsonSerializer<Any>?,
            elementTypeSerializer: TypeSerializer?,
            elementValueSerializer: JsonSerializer<Any>?
        ): JsonSerializer<*>? = null

        override fun findMapLikeSerializer(
            config: SerializationConfig?,
            type: MapLikeType?,
            beanDesc: BeanDescription?,
            keySerializer: JsonSerializer<Any>?,
            elementTypeSerializer: TypeSerializer?,
            elementValueSerializer: JsonSerializer<Any>?
        ): JsonSerializer<*>? = null
    }

    private val jacksonSerializers = JacksonSerializers()

    override fun asJacksonSerializers(): Serializers = jacksonSerializers

    private inner class VersionCache<T : Any> {
        @Volatile
        private var cache: T? = null

        @Volatile
        private var cacheVersion: Int = -1

        private val lock = ReentrantLock()

        fun compute(block: () -> T): T {
            val oldCacheBeforeLock = cache
            val newVersionBeforeLock = version.get()

            if (oldCacheBeforeLock != null && newVersionBeforeLock == cacheVersion) {
                return oldCacheBeforeLock
            }
            return lock.withLock {
                val oldCacheAfterLock = cache
                val newVersionAfterLock = version.get()

                if (oldCacheAfterLock != null && newVersionAfterLock == cacheVersion) {
                    return oldCacheAfterLock
                }

                block().apply {
                    cache = this
                    cacheVersion = newVersionAfterLock
                }
            }
        }
    }

    private inner class JacksonDeserializers : Deserializers {
        // 由于本模块提供的 Deserializer 实际上没有反序列化功能，
        // 最终实现需要依赖其他模块或 Jackson 默认的反序列化器，
        // 所以需要调用 Jackson 的 findContextualValueDeserializer。
        // 然而这会首先查询本模块的反序列化器。如果不提前禁止，就会导致递归。
        private val disableDeserializerFindFunctions = ThreadLocal<Unit?>()

        private fun <T> findContextualValueDeserializer(
            type: Class<T>, property: BeanProperty?, context: DeserializationContext
        ): JsonDeserializer<T> {
            val javaType = context.constructType(type)
            return disableDeserializerFindFunctions.withValue {
                context.findContextualValueDeserializer(javaType, property)
            } as JsonDeserializer<T>
        }

        private inner class DeserializerResolver<T>(
            val type: Class<T>,
            val hintedType: Class<out T>,
            var parser: JsonParser,
            val context: DeserializationContext,
            val property: BeanProperty?,
//            val propertyType: Class<out T>
            val operations: MutableList<Operation> = mutableListOf()
        ) {
            fun resolve(): JsonDeserializer<out T>? {
                resolveByTypeHints(typeHints)?.let { return it }
                typeBasedCodecs.findDeserializer(type)?.let { return it.asJacksonDeserializer }

                val root = parser.readValueAsTree<JsonNode>()
                parser = root.traverse(parser.codec)

                val nextToken = parser.nextToken()
                tokenBasedCodecs.findTokenBasedDeserializer(type, nextToken)?.let { return it.asJacksonDeserializer }

                if (root is ObjectNode) {
                    // nameField -> name -> entry
                    val nameBasedEntries = nameBasedCodecs.findNameBasedEntries(type)
                    if (nameBasedEntries != null) {
                        var nameField: String? = null
                        var name: String? = null

                        var deserializer: AbstractCodecEntry<T>.AbstractCodecRegistration? = null
                        for ((currentNameField, currentNameEntries) in nameBasedEntries) {
                            val currentName = root[currentNameField]?.textValue() ?: continue
                            val entry = currentNameEntries[currentName] ?: continue

                            require(deserializer === null) {
                                "Multiple deserialize codecs found for $type with: $currentNameField = $currentName and $nameField = $name"
                            }

                            nameField = currentNameField
                            name = currentName

                            deserializer = entry.deserializer ?: continue
                        }

                        deserializer?.let { return it.asJacksonDeserializer }
                    }

                    // nameField -> name -> hint
                    val nameBasedTypeHintClueCollector = NameBasedTypeHintClueCollector(type, hintedType)
                    val nameBasedTypeHintRegistrations = nameBasedTypeHintClueCollector.collect()
                    if (nameBasedTypeHintRegistrations.isNotEmpty()) {
                        return HintedJacksonDeserializer(nameBasedTypeHintClueCollector.currentType as Class<T>, nameBasedTypeHintRegistrations, property)
                    }
                }

                return resolveByTypeHints(fallbackTypeHints)
            }

            private fun resolveByTypeHints(typeHints: TypeHints): JsonDeserializer<out T>? {
                return typeHints.findTypeHint(type)?.let {
                    operations.add(it.operation)
                    findContextualValueDeserializer(it.value.hint, property, context)
                }
            }
        }

        // 结果未必准确。
        private fun <T> getDeserializerClassWeak(type: Class<T>): Class<out T>? {
            return type.inheritedClasses
                .firstNotNullOfOrNull {
                    getDeserializerClassStrong(it)
                } as Class<out T>?
        }

        private fun <T> getDeserializerClassStrong(type: Class<T>): Class<out T>? {
            typeHints.findTypeHint(type)?.value?.let { return type }
            typeBasedCodecs.findDeserializer(type)?.let { return type }

            tokenBasedCodecs.findTokenBasedCodecs(type)?.let { return type }

            nameBasedCodecs.findNameBasedEntries(type)?.let { return type }
            nameBasedTypeHints.findNameBasedTypeHints(type)?.let { return type }

            fallbackTypeHints.findTypeHint(type)?.let { return type }
            return null
        }

        override fun findArrayDeserializer(
            type: ArrayType,
            config: DeserializationConfig?, beanDesc: BeanDescription?,
            elementTypeDeserializer: TypeDeserializer?, elementDeserializer: JsonDeserializer<*>?
        ): JsonDeserializer<*>? = findDeserializer(type)

        override fun findBeanDeserializer(
            type: JavaType,
            config: DeserializationConfig?, beanDesc: BeanDescription?
        ): JsonDeserializer<*>? = findDeserializer(type)

        override fun findCollectionDeserializer(
            type: CollectionType,
            config: DeserializationConfig?, beanDesc: BeanDescription?,
            elementTypeDeserializer: TypeDeserializer?,
            elementDeserializer: JsonDeserializer<*>?
        ): JsonDeserializer<*>? = findDeserializer(type)

        override fun findCollectionLikeDeserializer(
            type: CollectionLikeType,
            config: DeserializationConfig, beanDesc: BeanDescription,
            elementTypeDeserializer: TypeDeserializer?,
            elementDeserializer: JsonDeserializer<*>?
        ): JsonDeserializer<*>? = findDeserializer(type)

        override fun findEnumDeserializer(
            type: Class<*>,
            config: DeserializationConfig?, beanDesc: BeanDescription?
        ): JsonDeserializer<*>? = null

        override fun findTreeNodeDeserializer(
            nodeType: Class<out JsonNode>,
            config: DeserializationConfig?, beanDesc: BeanDescription?
        ): JsonDeserializer<*>? = findDeserializer(nodeType)

        override fun findReferenceDeserializer(
            refType: ReferenceType,
            config: DeserializationConfig?, beanDesc: BeanDescription,
            contentTypeDeserializer: TypeDeserializer?, contentDeserializer: JsonDeserializer<*>?
        ): JsonDeserializer<*>? = findDeserializer(refType)

        override fun findMapDeserializer(
            type: MapType,
            config: DeserializationConfig?, beanDesc: BeanDescription?,
            keyDeserializer: KeyDeserializer?,
            elementTypeDeserializer: TypeDeserializer?,
            elementDeserializer: JsonDeserializer<*>?
        ): JsonDeserializer<*>? = findDeserializer(type)

        override fun findMapLikeDeserializer(
            type: MapLikeType,
            config: DeserializationConfig?, beanDesc: BeanDescription?,
            keyDeserializer: KeyDeserializer?,
            elementTypeDeserializer: TypeDeserializer?,
            elementDeserializer: JsonDeserializer<*>?
        ): JsonDeserializer<*>? = findDeserializer(type)

        private inner class HintedJacksonDeserializer<T>(
            private val type: Class<T>,
            private val typeHintRegistrations: List<NameBasedTypeHints.MutableHintRegistration<out T, out T>>,
            private val property: BeanProperty?
        ) : StdDeserializer<T>(type) {
            private val deserializerCache = VersionCache<JsonDeserializer<out T>>()

            override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
                val root = parser.readValueAsTree<JsonNode>()
                check(root is ObjectNode) { "Expected ObjectNode, but got ${root::class.java}" }

                for (registration in typeHintRegistrations) {
                    val nameClue = registration.nameClue
                    val nameNode = root[nameClue.nameField] as? TextNode ?: throw NoSuchElementException(
                        "Cannot find name field ${nameClue.nameField} (due to ${registration.operation.description}) in $root"
                    )

                    check(nameNode.textValue() == nameClue.name) {
                        "Conflict name field for $type: ${nameClue.nameField} -> ${nameNode.textValue()} (from deserialized) " +
                                "vs ${nameClue.name} (from clues due to ${registration.operation.description})"
                    }
                    if (!nameClue.visible) {
                        root.remove(nameClue.nameField)
                    }
                }

                val deserializer = deserializerCache.compute {
                    findContextualValueDeserializer(type, property, context)
                }

                return deserializer.deserialize(parser, context)
            }
        }

        private inner class DynamicJacksonDeserializer<T>(
            private val type: Class<in T>,
            private val hintedType: Class<T>
        ) : StdDeserializer<T>(hintedType), ContextualDeserializer {
            private val deserializerCache = VersionCache<MutableMap<ClassKey<*, *>, JsonDeserializer<*>>>()

            private inner class ContextualDynamicDeserializer(private val property: BeanProperty?) : JsonDeserializer<T>() {
                override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
                    return deserializeContextually(parser, context, property)
                }
            }

            override fun createContextual(ctxt: DeserializationContext?, property: BeanProperty?): JsonDeserializer<*> {
                return ContextualDynamicDeserializer(property)
            }

            override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
                return deserializeContextually(parser, context, property = null)
            }

            private fun deserializeContextually(parser: JsonParser, context: DeserializationContext, property: BeanProperty?): T {
                var finalParser = parser

                val finalType: Class<in T> = property?.type?.rawClass as Class<in T>? ?: type
                val finalHintedType = hintedType

                val key = ClassKey(finalType, finalHintedType)

                val finalDeserializer = deserializerCache.compute { ConcurrentHashMap() }
                    .computeIfAbsent(key) {
                        val resolver = DeserializerResolver(
                            type = finalType,
                            hintedType = finalHintedType,
                            parser = finalParser,
                            context = context,
                            property = property
                        )
                        val deserializer = resolver.resolve()

                        finalParser = resolver.parser
                        deserializer ?: findContextualValueDeserializer(finalType, property, context)
                    } as JsonDeserializer<out T>

                return finalDeserializer.deserialize(finalParser, context)
            }
        }

        val dynamicDeserializers = ConcurrentHashMap<Class<*>, DynamicJacksonDeserializer<*>>()

        fun findDeserializer(rawClass: Class<*>): JsonDeserializer<*>? {
            if (disableDeserializerFindFunctions.get() != null) {
                return null
            }

            dynamicDeserializers[rawClass]?.let { return it }

            getDeserializerClassWeak(rawClass)?.let {
                return dynamicDeserializers.computeIfAbsent(rawClass) { _ -> DynamicJacksonDeserializer(it, rawClass as Class<Nothing>) }
            }

            return null
        }

        fun findDeserializer(type: JavaType): JsonDeserializer<*>? = findDeserializer(type.rawClass)
    }

    private val jacksonDeserializers = JacksonDeserializers()

    override fun asJacksonDeserializers(): Deserializers = jacksonDeserializers

    private inner class JacksonModule : SimpleModule() {
        override fun getModuleName(): String = "xiaoming-serialization"
        override fun version(): Version = Jacksons.createModuleVersion(moduleName)

        override fun setupModule(context: SetupContext) {
            context.addSerializers(jacksonSerializers)
            context.addDeserializers(jacksonDeserializers)
        }
    }

    private val jacksonModule = JacksonModule()

    override fun asJacksonModule(): Module = jacksonModule

    override fun invalidateCache() {
        version.incrementAndGet()

        jacksonDeserializers.dynamicDeserializers.clear()
        jacksonSerializers.dynamicSerializers.clear()
    }
}