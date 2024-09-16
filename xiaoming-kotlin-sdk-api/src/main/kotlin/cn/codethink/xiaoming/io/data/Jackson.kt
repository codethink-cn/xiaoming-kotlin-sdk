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

import cn.codethink.xiaoming.common.AnyMatcher
import cn.codethink.xiaoming.common.Data
import cn.codethink.xiaoming.common.DefaultDataDeserializer
import cn.codethink.xiaoming.common.DefaultDataSerializer
import cn.codethink.xiaoming.common.DefaultPluginSubjectMatcher
import cn.codethink.xiaoming.common.DefaultSegmentIdMatcher
import cn.codethink.xiaoming.common.LiteralSegmentIdMatcher
import cn.codethink.xiaoming.common.LiteralStringMatcher
import cn.codethink.xiaoming.common.MATCHER_TYPE_ANY
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.PACKET_TYPE_RECEIPT
import cn.codethink.xiaoming.common.PACKET_TYPE_REQUEST
import cn.codethink.xiaoming.common.PERMISSION_MATCHER_TYPE_DEFAULT
import cn.codethink.xiaoming.common.PERMISSION_MATCHER_TYPE_LITERAL
import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.RegexStringMatcher
import cn.codethink.xiaoming.common.SEGMENT_ID_MATCHER_TYPE_DEFAULT
import cn.codethink.xiaoming.common.SEGMENT_ID_MATCHER_TYPE_LITERAL
import cn.codethink.xiaoming.common.STRING_MATCHER_TYPE_LITERAL
import cn.codethink.xiaoming.common.STRING_MATCHER_TYPE_REGEX
import cn.codethink.xiaoming.common.STRING_MATCHER_TYPE_WILDCARD
import cn.codethink.xiaoming.common.SUBJECT_MATCHER_TYPE_DEFAULT_PLUGIN
import cn.codethink.xiaoming.common.SUBJECT_TYPE_PLUGIN
import cn.codethink.xiaoming.common.SUBJECT_TYPE_PROTOCOL
import cn.codethink.xiaoming.common.SdkSubject
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.WildcardStringMatcher
import cn.codethink.xiaoming.permission.DefaultPermissionMatcher
import cn.codethink.xiaoming.permission.LiteralPermissionMatcher
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.databind.module.SimpleModule
import java.lang.reflect.Modifier

/**
 * An annotation that marks a class as a [Data] class that should be serialized
 * in the default way.
 *
 * @author Chuanwise
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultSerialization

/**
 * A Jackson annotation introspector that can create serializers and deserializers
 * of [Data] objects automatically.
 *
 * Notice that if the subclasses of [Data] class is marked with [DefaultSerialization],
 * it will not be serialized by [DefaultDataSerializer].
 *
 * Usage:
 *
 * ```kt
 * val mapper: ObjectMapper = jacksonObjectMapper().apply {
 *     setAnnotationIntrospector(AnnotationIntrospector.pair(
 *         // This class overrides some features of Jackson annotations.
 *         PlatformAnnotationIntrospector(),
 *         JacksonAnnotationIntrospector()
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

    override fun findSerializer(annotated: Annotated): Any? {
        if (Data::class.java.isAssignableFrom(annotated.rawType) &&
            annotated.getAnnotation(DefaultSerialization::class.java) == null
        ) {
            return DefaultDataSerializer
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
    "PlatformModule", XiaomingJacksonModuleVersion
) {
    inner class Deserializers {
        val packet = polymorphic<Packet> {
            dataType<RequestPacket>(PACKET_TYPE_REQUEST)
            dataType<ReceiptPacket>(PACKET_TYPE_RECEIPT)
        }
        val subject = polymorphic<Subject> {
            dataType<SdkSubject>(SUBJECT_TYPE_PROTOCOL)
            dataType<PluginSubject>(SUBJECT_TYPE_PLUGIN)
        }
        val matcher = polymorphic<Matcher<out Any?>> {
            subType<AnyMatcher>(MATCHER_TYPE_ANY)

            // String matchers.
            subType<WildcardStringMatcher>(STRING_MATCHER_TYPE_WILDCARD)
            subType<RegexStringMatcher>(STRING_MATCHER_TYPE_REGEX)
            subType<LiteralStringMatcher>(STRING_MATCHER_TYPE_LITERAL)

            // SegmentId matchers.
            subType<DefaultSegmentIdMatcher>(SEGMENT_ID_MATCHER_TYPE_DEFAULT)
            subType<LiteralSegmentIdMatcher>(SEGMENT_ID_MATCHER_TYPE_LITERAL)

            // Plugin subject matchers.
            dataType<DefaultPluginSubjectMatcher>(SUBJECT_MATCHER_TYPE_DEFAULT_PLUGIN)

            // Permission matchers.
            dataType<LiteralPermissionMatcher>(PERMISSION_MATCHER_TYPE_LITERAL)
            dataType<DefaultPermissionMatcher>(PERMISSION_MATCHER_TYPE_DEFAULT)
        }
    }
    val deserializers = Deserializers()
}
