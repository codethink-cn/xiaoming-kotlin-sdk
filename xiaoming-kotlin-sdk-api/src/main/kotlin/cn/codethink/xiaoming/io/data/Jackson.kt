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
import cn.codethink.xiaoming.common.DataDeserializerModifier
import cn.codethink.xiaoming.common.DefaultPluginSubjectMatcher
import cn.codethink.xiaoming.common.DefaultSegmentIdMatcher
import cn.codethink.xiaoming.common.FIELD_TYPE
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.LiteralSegmentIdMatcher
import cn.codethink.xiaoming.common.LiteralStringMatcher
import cn.codethink.xiaoming.common.LongId
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.PlatformSubject
import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.RegexStringMatcher
import cn.codethink.xiaoming.common.SdkSubject
import cn.codethink.xiaoming.common.StringId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.WildcardStringMatcher
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.prependOrNull
import cn.codethink.xiaoming.io.packet.Packet
import cn.codethink.xiaoming.io.packet.ReceiptPacket
import cn.codethink.xiaoming.io.packet.RequestPacket
import cn.codethink.xiaoming.permission.DefaultPermissionMatcher
import cn.codethink.xiaoming.permission.LiteralPermissionMatcher
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.Module
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

class DeserializerModule(
    private val name: String = "DeserializerModule",
    private val version: Version = Version.unknownVersion(),
    logger: KLogger = KotlinLogging.logger { }
) : Module() {
    override fun getModuleName(): String = name
    override fun version(): Version = version

    val deserializers = PolymorphicDeserializers(logger)
    val deserializerModifier = DataDeserializerModifier

    override fun setupModule(context: SetupContext) {
        context.addDeserializers(deserializers)
        context.addBeanDeserializerModifier(deserializerModifier)
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
        name<PlatformSubject>()
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