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

import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.deser.Deserializers.Base
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.type.ArrayType
import com.fasterxml.jackson.databind.type.CollectionLikeType
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.MapLikeType
import com.fasterxml.jackson.databind.type.MapType
import com.fasterxml.jackson.databind.type.ReferenceType
import io.github.oshai.kotlinlogging.KLogger
import java.lang.reflect.Modifier
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class MutablePolymorphicDeserializersImpl(
    private val logger: KLogger
) : Base(), MutablePolymorphicDeserializers {
    private val lock = ReentrantReadWriteLock()

    override val registrations: MutableList<Registration<JsonDeserializer<*>>> = CopyOnWriteArrayList()
    override val elements: Collection<JsonDeserializer<*>> get() = registrations.map { it.value }

    override val size: Int get() = registrations.size
    override val isEmpty: Boolean get() = registrations.isEmpty()

    private interface DeserializerRegistration<T> : Registration<JsonDeserializer<out T>> {
        val targetClass: Class<T>
    }

    private class NameBasedDeserializerRegistration<T>(
        val typeName: String,
        val typeNameField: String,
        val typeNameVisible: Boolean = DEFAULT_TYPE_NAME_VISIBLE,
        override val targetClass: Class<T>,
        override val value: JsonDeserializer<out T>,
        override val subject: SubjectDescriptor
    ) : DeserializerRegistration<T>

    // Class -> Type Name Field -> Type Name
    private val nameBasedDeserializerRegistrations =
        mutableMapOf<Class<*>, MutableMap<String, MutableMap<String, NameBasedDeserializerRegistration<*>>>>()

    private class TokenBasedDeserializerRegistration<T>(
        val token: JsonToken,
        override val targetClass: Class<T>,
        override val value: JsonDeserializer<out T>,
        override val subject: SubjectDescriptor
    ) : DeserializerRegistration<T>

    // Class -> Token -> Deserializer
    private val tokenBasedDeserializerRegistrations =
        mutableMapOf<Class<*>, MutableMap<JsonToken, TokenBasedDeserializerRegistration<*>>>()

    private class TypeBasedDeserializerRegistration<T>(
        override val targetClass: Class<T>,
        override val value: JsonDeserializer<out T>,
        override val subject: SubjectDescriptor
    ) : DeserializerRegistration<T>

    // Class -> Deserializer
    private val typeBasedDeserializerRegistrations = mutableMapOf<Class<*>, TypeBasedDeserializerRegistration<*>>()

    @DataDeserializer
    @Suppress("UNCHECKED_CAST")
    private inner class DynamicDeserializer<T>(private val type: Class<T>) : StdDeserializer<T>(type) {
        private fun <U : T> findDeserializerNoLock(
            type: Class<U>,
            parser: JsonParser,
            throwException: Boolean
        ): JsonDeserializer<U>? {
            // Check if the type registered.
            val typeBased = typeBasedDeserializerRegistrations[type]
            if (typeBased != null) {
                return typeBased.value as JsonDeserializer<U>
            }

            // Check if some kinds of next token registered.
            val tokenBased = tokenBasedDeserializerRegistrations[type]
            if (tokenBased != null) {
                val nextToken = parser.nextToken()
                val registration = tokenBased[nextToken]
                if (registration != null) {
                    return registration.value as JsonDeserializer<U>
                }
            }

            // Check if the type name field exists.
            val nameBased = nameBasedDeserializerRegistrations[type]
                ?: if (throwException) {
                    throw NoSuchElementException("No deserializer found for type $type.")
                } else {
                    return null
                }

            // Choose an acceptable type name field.
            var acceptableTypeNameField: String? = null
            var acceptableTypeName: String? = null

            val node = parser.readValueAsTree<ObjectNode>()
            for (typeNameField in nameBased.keys) {
                node[typeNameField]?.let {
                    if (acceptableTypeNameField != null) {
                        if (throwException) {
                            throw IllegalStateException(
                                "Unambiguous type information, at least 2 type name field candidate for type ${type.name}: " +
                                        "$acceptableTypeNameField ($acceptableTypeName) and $typeNameField (${it.asText()})."
                            )
                        } else {
                            return null
                        }
                    }

                    acceptableTypeNameField = typeNameField
                    acceptableTypeName = it.asText()
                }
            }
            if (acceptableTypeNameField == null) {
                if (throwException) {
                    throw NoSuchElementException(
                        "No deserializer found for type $type, acceptable type name fields: ${nameBased.keys}."
                    )
                } else {
                    return null
                }
            }

            val registrationCandidates = nameBased[acceptableTypeNameField]!!
            val registration = registrationCandidates[acceptableTypeName]
                ?: if (throwException) {
                    throw NoSuchElementException(
                        "No deserializer found for type $type, " +
                                "acceptable type names: ${registrationCandidates.keys} for type name field $acceptableTypeNameField."
                    )
                } else {
                    return null
                }

            return registration.value as JsonDeserializer<U>
        }

        override fun deserialize(parser: JsonParser, context: DeserializationContext): T = lock.read {
            // To prevent read a lot of times causing read null, read the node first.
            val node = parser.readValueAsTree<JsonNode>()

            // If it presents, use it.
            if (hasDeserializerFor(type, restrict = true)) {
                return@read findDeserializerNoLock(
                    type, node.traverse(parser.codec), throwException = true
                )!!.deserialize(node.traverse(parser.codec), context) as T
            }

            // Try to find a subclass deserializer.
            val subclasses = HashSet<Class<out T>>()
            for (subclass in typeBasedDeserializerRegistrations.keys) {
                if (type.isAssignableFrom(subclass)) {
                    subclasses.add(subclass as Class<out T>)
                }
            }
            for (subclass in nameBasedDeserializerRegistrations.keys) {
                if (type.isAssignableFrom(subclass)) {
                    subclasses.add(subclass as Class<out T>)
                }
            }
            for (subclass in tokenBasedDeserializerRegistrations.keys) {
                if (type.isAssignableFrom(subclass)) {
                    subclasses.add(subclass as Class<out T>)
                }
            }

            val deserializerCandidates = subclasses.mapNotNull {
                findDeserializerNoLock(
                    it, node.traverse(parser.codec), throwException = false
                )
            }

            if (deserializerCandidates.isEmpty()) {
                throw NoSuchElementException("No deserializer found for type $type.")
            } else if (deserializerCandidates.size == 1) {
                deserializerCandidates.single().deserialize(node.traverse(parser.codec), context) as T
            } else {
                throw IllegalStateException(
                    "Ambiguous deserializers for type $type: " +
                            "${deserializerCandidates.joinToString { handledType().toString() }}."
                )
            }
        }
    }

    private val deserializerCache = mutableMapOf<Class<*>, DynamicDeserializer<*>>()

    private fun hasDeserializerFor(type: Class<*>, restrict: Boolean): Boolean = lock.read {
        // If there is a deserializer for the type, return true.
        val typeBasedRegistration = typeBasedDeserializerRegistrations[type]
        val nameBasedRegistrations = nameBasedDeserializerRegistrations[type]
        val tokenBasedRegistrations = tokenBasedDeserializerRegistrations[type]

        if (!nameBasedRegistrations.isNullOrEmpty() ||
            !tokenBasedRegistrations.isNullOrEmpty() ||
            typeBasedRegistration != null
        ) {
            return@read true
        }

        if (!restrict) {
            // If not, but some subclasses have deserializers, also return true.
            for (thatType in nameBasedDeserializerRegistrations.keys) {
                if (type.isAssignableFrom(thatType)) {
                    return@read true
                }
            }
            for (thatType in tokenBasedDeserializerRegistrations.keys) {
                if (type.isAssignableFrom(thatType)) {
                    return@read true
                }
            }
            for (thatType in typeBasedDeserializerRegistrations.keys) {
                if (type.isAssignableFrom(thatType)) {
                    return@read true
                }
            }
        }

        false
    }

    private fun findDeserializer(type: JavaType): JsonDeserializer<*>? = findDeserializer(type.rawClass)

    private fun findDeserializer(rawClass: Class<*>): JsonDeserializer<*>? = lock.read {
        if (hasDeserializerFor(rawClass, restrict = false)) {
            deserializerCache.computeIfAbsent(rawClass) { DynamicDeserializer(rawClass) }
        } else {
            deserializerCache.remove(rawClass)
            null
        }
    }

    override fun <T> registerNameBasedDeserializer(
        type: Class<T>,
        typeNameField: String,
        typeName: String,
        deserializer: JsonDeserializer<out T>,
        subject: SubjectDescriptor,
        typeNameVisible: Boolean,
        replacePrevious: Boolean
    ): Boolean = lock.write {
        val registrations = nameBasedDeserializerRegistrations
            .computeIfAbsent(type) { mutableMapOf() }
            .computeIfAbsent(typeNameField) { mutableMapOf() }
        if (registrations.containsKey(typeName) && !replacePrevious) {
            return@write false
        }

        val newRegistration =
            NameBasedDeserializerRegistration(typeName, typeNameField, typeNameVisible, type, deserializer, subject)
        val oldRegistration = registrations.put(typeName, newRegistration)

        this.registrations.add(newRegistration)

        if (oldRegistration != null) {
            logger.warn {
                "Overriding deserializer for type $type (type name $typeName, type name field $typeNameField) " +
                        "previous registered by ${oldRegistration.subject} by $subject."
            }
            this.registrations.remove(oldRegistration)
        }
        return@write true
    }

    override fun <T> registerTokenBasedDeserializer(
        type: Class<T>,
        token: JsonToken,
        deserializer: JsonDeserializer<out T>,
        subject: SubjectDescriptor,
        replacePrevious: Boolean
    ): Boolean = lock.write {
        val registrations = tokenBasedDeserializerRegistrations.computeIfAbsent(type) { mutableMapOf() }
        if (registrations.containsKey(token) && !replacePrevious) {
            return@write false
        }

        val newRegistration = TokenBasedDeserializerRegistration(token, type, deserializer, subject)
        val oldRegistration = registrations.put(token, newRegistration)

        this.registrations.add(newRegistration)

        if (oldRegistration != null) {
            logger.warn {
                "Overriding deserializer for type $type (token $token) " +
                        "previous registered by ${oldRegistration.subject} by $subject."
            }
            this.registrations.remove(oldRegistration)
        }
        return@write true
    }

    override fun <T> registerTypeBasedDeserializer(
        type: Class<in T>,
        targetClass: Class<T>,
        deserializer: JsonDeserializer<out T>,
        subject: SubjectDescriptor
    ): Boolean = lock.write {
        val newRegistration = TypeBasedDeserializerRegistration(targetClass, deserializer, subject)
        val oldRegistration = typeBasedDeserializerRegistrations.put(type, newRegistration)

        this.registrations.add(newRegistration)

        if (oldRegistration != null) {
            logger.warn {
                "Overriding deserializer for type $type previous registered by ${oldRegistration.subject} by $subject."
            }
            this.registrations.remove(oldRegistration)
        }
        return@write true
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

    override fun hasDeserializerFor(
        config: DeserializationConfig?,
        valueType: Class<*>
    ): Boolean = hasDeserializerFor(valueType, restrict = false)

    private fun unregisterBySubjectRecursively(map: MutableMap<*, *>, subject: SubjectDescriptor): Boolean {
        var changed = false

        val outerIterator = map.values.iterator()
        while (outerIterator.hasNext()) {
            val outerValues = outerIterator.next()

            changed = when (outerValues) {
                is MutableMap<*, *> -> {
                    unregisterBySubjectRecursively(outerValues, subject).apply {
                        if (outerValues.isEmpty()) {
                            outerIterator.remove()
                        }
                    }
                }

                is Registration<*> -> {
                    if (outerValues.subject == subject) {
                        outerIterator.remove()
                        this.registrations.remove(outerValues)
                        true
                    } else {
                        false
                    }
                }

                else -> false
            } || changed
        }

        return changed
    }

    override fun unregisterAll(subject: SubjectDescriptor): Boolean {
        var changed: Boolean

        lock.write {
            changed = unregisterBySubjectRecursively(nameBasedDeserializerRegistrations, subject)
            changed = unregisterBySubjectRecursively(tokenBasedDeserializerRegistrations, subject) || changed
            changed = unregisterBySubjectRecursively(typeBasedDeserializerRegistrations, subject) || changed
        }

        return changed
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> defaultDataDeserializer(): JsonDeserializer<T>? {
    val javaClass = T::class.java
    if (Data::class.java.isAssignableFrom(javaClass)) {
        val modifiers = javaClass.modifiers
        if (!(Modifier.isAbstract(modifiers) || javaClass.isInterface || javaClass.isAnnotation)) {
            return DefaultDataDeserializer(javaClass as Class<out Data>) as JsonDeserializer<T>
        }
    }
    return null
}

inline fun <reified T> defaultDeserializer() = defaultDataDeserializer<T>()
    ?: ForwardDeserializer<T>()

inline fun <reified T> typeNameFromAnnotationOrFail() = T::class.java.getAnnotation(JsonTypeName::class.java)?.value
    ?: throw NoSuchElementException("Type name required for type ${T::class.java.name}.")

/**
 * @author Chuanwise
 * @see names
 * @see tokens
 */
class PolymorphicDeserializersRegisterer(
    val deserializers: MutablePolymorphicDeserializers,
    val subject: SubjectDescriptor
)

fun MutablePolymorphicDeserializers.subject(
    subject: SubjectDescriptor,
    register: PolymorphicDeserializersRegisterer.() -> Unit
) {
    PolymorphicDeserializersRegisterer(this, subject).apply(register)
}

/**
 * @author Chuanwise
 * @see names
 */
class NameBasedPolymorphicDeserializersRegisterer<T>(
    val type: Class<T>,
    val typeNameField: String,
    val registerer: PolymorphicDeserializersRegisterer
)

/**
 * Tool allows developers register type-name-based deserializers in Kotlin DSL style.
 *
 * Example:
 *
 * ```kt
 * fun PolymorphicDeserializers.registerDemoDeserializers(subject: Subject) {
 *     names<Foo>(FIELD_TYPE, subject) {
 *         name<Bar>()
 *     }
 * }
 * ```
 */
inline fun <reified T> PolymorphicDeserializersRegisterer.names(
    typeNameField: String,
    register: NameBasedPolymorphicDeserializersRegisterer<T>.() -> Unit
) = NameBasedPolymorphicDeserializersRegisterer(T::class.java, typeNameField, this).apply(register)

inline fun <reified T> NameBasedPolymorphicDeserializersRegisterer<in T>.names(
    typeNameField: String,
    typeName: String = typeNameFromAnnotationOrFail<T>(),
    deserializer: JsonDeserializer<T> = defaultDeserializer(),
    typeNameVisible: Boolean = DEFAULT_TYPE_NAME_VISIBLE,
    replacePrevious: Boolean = DEFAULT_REPLACE_PREVIOUS,
    register: NameBasedPolymorphicDeserializersRegisterer<T>.() -> Unit
): NameBasedPolymorphicDeserializersRegisterer<T> {
    name(typeName, deserializer, typeNameVisible, replacePrevious)
    return NameBasedPolymorphicDeserializersRegisterer(T::class.java, typeNameField, registerer).apply(register)
}

inline fun <reified T> NameBasedPolymorphicDeserializersRegisterer<in T>.name(
    typeName: String = typeNameFromAnnotationOrFail<T>(),
    deserializer: JsonDeserializer<T> = defaultDeserializer(),
    typeNameVisible: Boolean = DEFAULT_TYPE_NAME_VISIBLE,
    replacePrevious: Boolean = DEFAULT_REPLACE_PREVIOUS
): Boolean = registerer.deserializers.registerNameBasedDeserializer(
    type, typeNameField, typeName, deserializer, registerer.subject, typeNameVisible, replacePrevious
)

/**
 * @author Chuanwise
 * @see names
 */
class TokenBasedPolymorphicDeserializersRegisterer<T>(
    val type: Class<T>,
    val registerer: PolymorphicDeserializersRegisterer
)

/**
 * Tool allows developers register token-based deserializers in Kotlin DSL style.
 *
 * Example:
 *
 * ```kt
 * fun PolymorphicDeserializers.registerDemoDeserializers(subject: Subject) {
 *     tokens<Foo>(subject) {
 *         token<Bar>(JsonToken.VALUE_STRING)
 *     }
 * }
 * ```
 */
inline fun <reified T> PolymorphicDeserializersRegisterer.tokens(
    register: TokenBasedPolymorphicDeserializersRegisterer<T>.() -> Unit
) = TokenBasedPolymorphicDeserializersRegisterer(T::class.java, this).apply(register)

inline fun <reified T> TokenBasedPolymorphicDeserializersRegisterer<in T>.token(
    token: JsonToken,
    deserializer: JsonDeserializer<T> = defaultDeserializer(),
    replacePrevious: Boolean = DEFAULT_REPLACE_PREVIOUS
): Boolean = registerer.deserializers.registerTokenBasedDeserializer(
    type, token, deserializer, registerer.subject, replacePrevious
)

inline fun <reified T, reified U : T> PolymorphicDeserializersRegisterer.type(
    targetType: Class<U> = U::class.java,
    deserializer: JsonDeserializer<U> = defaultDeserializer()
): Boolean = this.deserializers.registerTypeBasedDeserializer(T::class.java, targetType, deserializer, subject)
