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

@file:JvmName("Jackson")

package cn.codethink.xiaoming.io.data

import cn.codethink.xiaoming.common.CurrentProtocolSubject
import cn.codethink.xiaoming.common.MapRegistrations
import cn.codethink.xiaoming.common.PACKET_FIELD_TYPE
import cn.codethink.xiaoming.common.PACKET_TYPE_RECEIPT
import cn.codethink.xiaoming.common.PACKET_TYPE_REQUEST
import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.PolymorphicDeserializingException
import cn.codethink.xiaoming.common.ProtocolSubject
import cn.codethink.xiaoming.common.ReflectDataDeserializer
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.SUBJECT_FIELD_TYPE
import cn.codethink.xiaoming.common.SUBJECT_TYPE_PLUGIN
import cn.codethink.xiaoming.common.SUBJECT_TYPE_PROTOCOL
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.prependOrNull
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * A deserializer that can deserialize different types of objects based
 * on the value of a single field called [typeNameField].
 *
 * @author Chuanwise
 */
class PolymorphicDeserializerManager<T>(
    private val type: Class<T>,
    private val typeNameField: String
) : StdDeserializer<T>(type) {
    companion object {
        const val DEFAULT_TYPE_NAME_VISIBLE = true
    }

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
            ?: throw NoSuchElementException("No deserializer found for type name $typeName (type: ${type.name}).")

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
        typeNameVisible: Boolean = DEFAULT_TYPE_NAME_VISIBLE
    ): DeserializerRegistration? {
        val registration = DeserializerRegistration(typeName, typeNameVisible, deserializer, subject)
        return registrations.register(typeName, registration)
    }

    fun unregisterDeserializerByType(typeName: String): DeserializerRegistration? =
        registrations.unregisterByKey(typeName)

    fun unregisterDeserializerBySubject(subject: Subject): Boolean = registrations.unregisterBySubject(subject)
}

/**
 * A Jackson module that contains all settings of remote-core need.
 *
 * @author Chuanwise
 */
class PlatformModule : SimpleModule(
    "PlatformModule",
    CurrentProtocolSubject.let {
        val version = it.version
        val snapshotInfo = version.preRelease.prependOrNull("-").orEmpty() + version.build.prependOrNull("-").orEmpty()
        Version(version.major, version.minor, version.patch, snapshotInfo, it.group, it.name)
    }
) {
    inner class Deserializers {
        val packet = registerPolymorphicDeserializerManager<Packet>(PACKET_FIELD_TYPE).apply {
            registerDeserializer(PACKET_TYPE_REQUEST, ReflectDataDeserializer<RequestPacket>(), CurrentProtocolSubject)
            registerDeserializer(PACKET_TYPE_RECEIPT, ReflectDataDeserializer<ReceiptPacket>(), CurrentProtocolSubject)
        }
        val subject = registerPolymorphicDeserializerManager<Subject>(SUBJECT_FIELD_TYPE).apply {
            registerDeserializer(
                SUBJECT_TYPE_PROTOCOL,
                ReflectDataDeserializer<ProtocolSubject>(),
                CurrentProtocolSubject
            )
            registerDeserializer(SUBJECT_TYPE_PLUGIN, ReflectDataDeserializer<PluginSubject>(), CurrentProtocolSubject)
        }

        private inline fun <reified T> registerPolymorphicDeserializerManager(typeNameField: String) =
            PolymorphicDeserializerManager(
                T::class.java, typeNameField
            ).apply { addDeserializer(T::class.java, this@apply) }
    }

    val deserializers = Deserializers()
}