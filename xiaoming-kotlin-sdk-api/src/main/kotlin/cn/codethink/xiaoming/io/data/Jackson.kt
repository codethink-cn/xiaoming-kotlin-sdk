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
import cn.codethink.xiaoming.common.FIELD_TYPE
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.LiteralSegmentIdMatcher
import cn.codethink.xiaoming.common.LiteralStringMatcher
import cn.codethink.xiaoming.common.LongId
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.RegexStringMatcher
import cn.codethink.xiaoming.common.SdkSubject
import cn.codethink.xiaoming.common.StringId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.WildcardStringMatcher
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.prependOrNull
import cn.codethink.xiaoming.permission.DefaultPermissionMatcher
import cn.codethink.xiaoming.permission.LiteralPermissionMatcher
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.reflect.Modifier

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
        if (Data::class.java.isAssignableFrom(annotated.rawType)) {
            return DefaultDataSerializer
        }
        return null
    }
}

class DeserializerModule(
    private val name: String = "DeserializerModule",
    private val version: Version = Version.unknownVersion(),
    private val logger: KLogger = KotlinLogging.logger { }
) : Module() {
    override fun getModuleName(): String = name
    override fun version(): Version = version

    val deserializers = PolymorphicDeserializers(logger)

    override fun setupModule(context: SetupContext) {
        context.addDeserializers(deserializers)
    }
}

val XiaomingJacksonModuleVersion = XiaomingSdkSubject.let {
    val version = it.version
    val snapshotInfo = version.preRelease.prependOrNull("-").orEmpty() + version.build.prependOrNull("-").orEmpty()
    Version(version.major, version.minor, version.patch, snapshotInfo, it.group, it.name)
}

fun PolymorphicDeserializers.registerPlatformDeserializers(subject: Subject) = subject(subject) {
    names<Packet>(FIELD_TYPE) {
        name<RequestPacket>()
        name<ReceiptPacket>()
    }
    names<Subject>(FIELD_TYPE) {
        name<SdkSubject>()
        name<PluginSubject>()
    }
    names<Matcher<Any?>>(FIELD_TYPE) {
        name<AnyMatcher>()

        // String matchers.
        name<WildcardStringMatcher>()
        name<RegexStringMatcher>()
        name<LiteralStringMatcher>()

        // SegmentId matchers.
        name<DefaultSegmentIdMatcher>()
        name<LiteralSegmentIdMatcher>()

        // Plugin subject matchers.
        name<DefaultPluginSubjectMatcher>()

        // Permission matchers.
        name<LiteralPermissionMatcher>()
        name<DefaultPermissionMatcher>()
    }
    tokens<Id> {
        token<LongId>(JsonToken.VALUE_NUMBER_INT)
        token<StringId>(JsonToken.VALUE_STRING)
    }
}