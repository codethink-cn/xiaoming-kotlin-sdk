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

package cn.codethink.xiaoming.api

import cn.codethink.xiaoming.event.EventPublishPolicy
import cn.codethink.xiaoming.message.Text
import cn.codethink.xiaoming.plugin.PluginDependency
import cn.codethink.xiaoming.plugin.PluginRequirement
import cn.codethink.xiaoming.plugin.PluginStateChangePolicy
import cn.codethink.xiaoming.serialization.CodecResolver
import cn.codethink.xiaoming.util.AndVersionPattern
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.Data
import cn.codethink.xiaoming.util.ExcludeVersionPattern
import cn.codethink.xiaoming.util.GreaterThanOrEqualVersionPattern
import cn.codethink.xiaoming.util.GreaterThanVersionPattern
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.IncludeVersionPattern
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.LessThanOrEqualVersionPattern
import cn.codethink.xiaoming.util.LessThanVersionPattern
import cn.codethink.xiaoming.util.LiteralSegmentIdPatternElement
import cn.codethink.xiaoming.util.MajorMinorVersionPrefixPattern
import cn.codethink.xiaoming.util.MajorVersionPrefixPattern
import cn.codethink.xiaoming.util.MutableStore
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.NamespaceIdPattern
import cn.codethink.xiaoming.util.NamingPolicy
import cn.codethink.xiaoming.util.NumericalId
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.OrVersionPattern
import cn.codethink.xiaoming.util.RegexSegmentIdPatternElement
import cn.codethink.xiaoming.util.SegmentId
import cn.codethink.xiaoming.util.SegmentIdPattern
import cn.codethink.xiaoming.util.SegmentIdPatternElement
import cn.codethink.xiaoming.util.Store
import cn.codethink.xiaoming.util.StringId
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Template
import cn.codethink.xiaoming.util.TextualId
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.TypeMeta
import cn.codethink.xiaoming.util.UniversalUniqueId
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionPattern
import java.lang.reflect.Type
import java.util.UUID
import java.util.function.Supplier
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

/**
 * 小明 [CoreApi]，是通过 API 模块主动调用 CORE 的桥梁。
 *
 * @author Chuanwise
 */
@InternalApi
interface CoreApi {
    companion object {
        @JvmStatic
        fun getInstance(): CoreApi = CoreApiInstance.get()
    }

    // Id
    fun createTextualId(string: String): TextualId

    fun toStringId(id: Id): StringId
    fun toNumericalId(id: Id): NumericalId
    fun toNamespaceId(id: Id): NamespaceId
    fun toSegmentId(id: Id): SegmentId

    fun createSegmentId(segments: List<String>): SegmentId
    fun createSegmentId(string: String): SegmentId

    fun createNumericalId(value: Long): NumericalId
    fun createNumericalId(value: Int): NumericalId

    fun createStringId(string: String): StringId

    fun createNamespaceId(group: SegmentId, name: SegmentId): NamespaceId
    fun createNamespaceId(string: String): NamespaceId

    fun createRandomUniversalUniqueId(): UniversalUniqueId
    fun createUniversalUniqueId(uuid: UUID): UniversalUniqueId

    // Template
    fun createTemplate(format: String): Template

    // Cause
    fun createCause(description: String, cause: Cause?): Cause

    // Operation
    fun createOperation(
        message: String,
        operator: SubjectDescriptor,
        cause: Cause?,
        time: Time,
        id: Id
    ): Operation

    // EventPublishPolicy
    fun createEventPublishPolicy(mutable: Boolean, interceptable: Boolean): EventPublishPolicy

    // Data
    fun createData(raw: MutableStore): Data

    // Time
    fun createUnixMillisecondsTime(milliseconds: Long): Time
    fun createUnixSecondsTime(seconds: Long): Time

    // PluginMetaMatcher
    fun createPluginDependency(string: String): PluginDependency

    fun createPluginDependency(
        id: NamespaceId,
        version: VersionPattern?,
        optional: Boolean
    ): PluginDependency

    fun createPluginRequirement(string: String): PluginRequirement
    fun createPluginRequirement(id: NamespaceId, version: VersionPattern?): PluginRequirement

    // StringMatcher
    fun createLiteralSegmentIdPatternElement(string: String): LiteralSegmentIdPatternElement
    fun createRegexStringMatcher(regex: Regex): RegexSegmentIdPatternElement

    // SegmentIdPattern
    fun createSegmentIdPattern(string: String): SegmentIdPattern
    fun createSegmentIdPattern(matchers: List<SegmentIdPatternElement>): SegmentIdPattern

    // NamespaceIdMatcher
    fun createNamespaceIdMatcher(group: SegmentIdPattern, name: SegmentIdPattern): NamespaceIdPattern
    fun parseNamespaceIdMatcher(string: String): NamespaceIdPattern

    // VersionPattern
    fun createVersionPattern(string: String): VersionPattern

    fun createAndVersionPattern(left: VersionPattern, right: VersionPattern): AndVersionPattern
    fun createOrVersionPattern(left: VersionPattern, right: VersionPattern): OrVersionPattern
    fun createIncludeVersionPattern(value: Version): IncludeVersionPattern
    fun createExcludeVersionPattern(value: Version): ExcludeVersionPattern
    fun createGreaterThanVersionPattern(version: Version): GreaterThanVersionPattern
    fun createGreaterThanOrEqualVersionPattern(version: Version): GreaterThanOrEqualVersionPattern
    fun createLessThanVersionPattern(version: Version): LessThanVersionPattern
    fun createLessThanOrEqualVersionPattern(version: Version): LessThanOrEqualVersionPattern
    fun createMajorVersionPrefixPattern(major: Int): MajorVersionPrefixPattern
    fun createMajorMinorVersionPrefixPattern(major: Int, minor: Int): MajorMinorVersionPrefixPattern

    // Version
    fun createVersion(major: Int, minor: Int, patch: Int, preRelease: String?, build: String?): Version
    fun createVersion(string: String): Version

    // Store
    fun createMapStore(map: MutableMap<String, Any?>): MutableStore
    fun createEmptyStore(): Store

    fun <T> createReadOnlyStoreProperty(
        store: Store, name: String?, meta: TypeMeta<T>?, namingPolicy: NamingPolicy?, defaultValueFactory: Supplier<T>?
    ): ReadOnlyProperty<Any?, T>

    fun <T> createReadWriteStoreProperty(
        store: MutableStore, name: String?, meta: TypeMeta<T>?, namingPolicy: NamingPolicy?, defaultValueFactory: Supplier<T>?
    ): ReadWriteProperty<Any?, T>

    // TypeMeta
    fun createTypeMeta(type: Type, nullable: Boolean): TypeMeta<*>

    // NamingPolicy
    fun getKebabCaseNamingPolicy(): NamingPolicy
    fun getLowerCamelCaseNamingPolicy(): NamingPolicy
    fun getUpperCamelCaseNamingPolicy(): NamingPolicy
    fun getSnakeCaseNamingPolicy(): NamingPolicy
    fun getUpperSnakeCaseNamingPolicy(): NamingPolicy
    fun getLowerCaseNamingPolicy(): NamingPolicy
    fun getLowerDotCaseNamingPolicy(): NamingPolicy

    // PluginStateChangePolicy
    fun createPluginStateChangePolicy(ignorePreviousError: Boolean, ignoreCurrentError: Boolean): PluginStateChangePolicy

    // CodecResolver
    fun findAndApplyInitializers(resolver: CodecResolver, operation: Operation, classLoader: ClassLoader?, replace: Boolean, visible: Boolean)

    // Text
    fun createText(string: String): Text
}