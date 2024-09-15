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

import cn.codethink.xiaoming.common.Data
import cn.codethink.xiaoming.common.DefaultDataDeserializer
import cn.codethink.xiaoming.common.PACKET_TYPE_RECEIPT
import cn.codethink.xiaoming.common.PACKET_TYPE_REQUEST
import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.SUBJECT_TYPE_PLUGIN
import cn.codethink.xiaoming.common.SUBJECT_TYPE_PROTOCOL
import cn.codethink.xiaoming.common.SdkSubject
import cn.codethink.xiaoming.common.Subject
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.databind.module.SimpleModule
import java.lang.reflect.Modifier

/**
 * A Jackson annotation introspector that can create deserializer of [Data] objects
 * automatically.
 *
 * Usage:
 *
 * ```kt
 * val mapper: ObjectMapper = jacksonObjectMapper().apply {
 *     setAnnotationIntrospector(AnnotationIntrospector.pair(
 *         JacksonAnnotationIntrospector(),
 *         PlatformAnnotationIntrospector()
 *     ))
 * }
 * ```
 *
 * @author Chuanwise
 */
class PlatformAnnotationIntrospector : NopAnnotationIntrospector() {
    @Suppress("UNCHECKED_CAST")
    override fun findDeserializer(annotated: Annotated): Any? {
        if (Data::class.java.isAssignableFrom(annotated.rawType)) {
            val modifiers = annotated.rawType.modifiers
            if (!(Modifier.isInterface(modifiers) || Modifier.isAbstract(modifiers))) {
                val rawClass: Class<out Data> = annotated.rawType as Class<out Data>
                return DefaultDataDeserializer(rawClass)
            }
        }
        return null
    }
}

/**
 * A Jackson module that contains all settings of remote-core need.
 *
 * @author Chuanwise
 */
class PlatformModule : SimpleModule(
    "PlatformModule", CurrentJacksonModuleVersion
) {
    inner class Deserializers {
        val packet = polymorphic<Packet> {
            subType<RequestPacket>(PACKET_TYPE_REQUEST)
            subType<ReceiptPacket>(PACKET_TYPE_RECEIPT)
        }
        val subject = polymorphic<Subject> {
            subType<SdkSubject>(SUBJECT_TYPE_PROTOCOL)
            subType<PluginSubject>(SUBJECT_TYPE_PLUGIN)
        }
    }
    val deserializers = Deserializers()
}
