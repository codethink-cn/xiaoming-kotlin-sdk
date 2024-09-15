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

import cn.codethink.xiaoming.common.CurrentProtocolSubject
import cn.codethink.xiaoming.common.CurrentSdkSubject
import cn.codethink.xiaoming.common.Data
import cn.codethink.xiaoming.common.DefaultDataDeserializer
import cn.codethink.xiaoming.common.MapRegistrations
import cn.codethink.xiaoming.common.PolymorphicDeserializingException
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.TYPE_FIELD_NAME
import cn.codethink.xiaoming.common.prependOrNull
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode

const val DEFAULT_TYPE_NAME_VISIBLE = true

/**
 * A deserializer that can deserialize different types of objects based
 * on the value of a single field called [typeNameField]. In the most situations,
 * it's [TYPE_FIELD_NAME].
 *
 * @author Chuanwise
 * @see polymorphic
 * @see subType
 */
class PolymorphicDeserializerManager<T>(
    private val type: Class<T>,
    private val typeNameField: String
) : StdDeserializer<T>(type) {
    inner class DeserializerRegistration(
        val typeName: String,
        val typeNameVisible: Boolean = DEFAULT_TYPE_NAME_VISIBLE,
        override val value: JsonDeserializer<out T>,
        override val subject: Subject
    ) : Registration<JsonDeserializer<out T>>

    private val registrations = MapRegistrations<String, JsonDeserializer<out T>, DeserializerRegistration>()

    override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
        val node = parser.readValueAsTree<ObjectNode>()
        val typeNameFieldNode = node.get(typeNameField)
            ?: throw NoSuchElementException("No type field found in $node.")

        val typeName = typeNameFieldNode.asText()
        val registration = registrations[typeName]
            ?: throw NoSuchElementException(
                "No deserializer found for type name $typeName (type: ${type.name}). " +
                        "Acceptable: ${registrations.toMap().keys}. " +
                        "Make sure using `manager.registerDeserializer(...)` registered, or " +
                        "loaded by service `PolymorphicDeserializerService`. "
            )

        if (!registration.typeNameVisible) {
            node.remove(typeNameField)
        }
        try {
            return registration.value.deserialize(node.traverse(parser.codec), context)
        } catch (exception: Exception) {
            throw PolymorphicDeserializingException(node, registration)
        }
    }

    fun registerDeserializer(
        typeName: String,
        deserializer: JsonDeserializer<out T>,
        subject: Subject,
        typeNameVisible: Boolean
    ): DeserializerRegistration? {
        val registration = DeserializerRegistration(typeName, typeNameVisible, deserializer, subject)
        return registrations.register(typeName, registration)
    }

    fun unregisterDeserializerByType(typeName: String): DeserializerRegistration? =
        registrations.unregisterByKey(typeName)

    fun unregisterDeserializerBySubject(subject: Subject): Boolean = registrations.unregisterBySubject(subject)
}

/**
 * Create and register an [PolymorphicDeserializerManager] to a Jackson [SimpleModule]
 * in Kotlin DSL style.
 *
 * Usage:
 *
 * ```kt
 * class SomeModule: SimpleModule() {
 *     val animalDeserializers = polymorphic<Animal> {
 *         // `this` is the PolymorphicDeserializerManager.
 *     }
 * }
 * ```
 *
 * @param T the type of object to be deserialized.
 * @param typeNameField the field name to the type name, default to [TYPE_FIELD_NAME].
 * @param loadServices load all related [PolymorphicDeserializerService] and register them,
 * default to `true`.
 * @param block the block to configure the [PolymorphicDeserializerManager].
 *
 * @see subType
 */
inline fun <reified T> SimpleModule.polymorphic(
    typeNameField: String = TYPE_FIELD_NAME,
    loadServices: Boolean = true,
    block: PolymorphicDeserializerManager<T>.() -> Unit = {}
) = PolymorphicDeserializerManager(T::class.java, typeNameField)
    .apply(block)
    .apply {
        if (loadServices) {
            filterPolymorphicDeserializerServices(T::class.java).forEach {
                registerDeserializer(it.typeName, it.deserializer, it.subject, it.typeNameVisible)
            }
        }
    }
    .apply { addDeserializer(T::class.java, this@apply) }

/**
 * Register a subtype deserializer to a [PolymorphicDeserializerManager]
 * USING [CurrentProtocolSubject].
 *
 * @see polymorphic
 */
inline fun <reified T : Data> PolymorphicDeserializerManager<in T>.subType(
    typeName: String,
    deserializer: JsonDeserializer<out T> = DefaultDataDeserializer<T>(),
    subject: Subject = CurrentProtocolSubject,
    typeNameVisible: Boolean = DEFAULT_TYPE_NAME_VISIBLE
) = registerDeserializer(typeName, deserializer, subject, typeNameVisible)

val CurrentJacksonModuleVersion = CurrentSdkSubject.let {
    val version = it.version
    val snapshotInfo = version.preRelease.prependOrNull("-").orEmpty() + version.build.prependOrNull("-").orEmpty()
    Version(version.major, version.minor, version.patch, snapshotInfo, it.group, it.name)
}