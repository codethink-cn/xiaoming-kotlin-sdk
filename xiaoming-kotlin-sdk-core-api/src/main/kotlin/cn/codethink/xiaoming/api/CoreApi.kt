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

package cn.codethink.xiaoming.api

import cn.codethink.xiaoming.event.EventPublishPolicy
import cn.codethink.xiaoming.plugin.PluginRequirement
import cn.codethink.xiaoming.plugin.PluginStateChangePolicy
import cn.codethink.xiaoming.serialization.CodecResolver
import cn.codethink.xiaoming.util.AndVersionMatcher
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.Data
import cn.codethink.xiaoming.util.ExcludeVersionMatcher
import cn.codethink.xiaoming.util.GreaterThanOrEqualVersionMatcher
import cn.codethink.xiaoming.util.GreaterThanVersionMatcher
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.IncludeVersionMatcher
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.LessThanOrEqualVersionMatcher
import cn.codethink.xiaoming.util.LessThanVersionMatcher
import cn.codethink.xiaoming.util.MajorMinorVersionPrefixMatcher
import cn.codethink.xiaoming.util.MajorVersionPrefixMatcher
import cn.codethink.xiaoming.util.MutableStore
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.NamespaceIdMatcher
import cn.codethink.xiaoming.util.NamingPolicy
import cn.codethink.xiaoming.util.NumericalId
import cn.codethink.xiaoming.util.OrVersionMatcher
import cn.codethink.xiaoming.util.PluginSubjectDescriptorMatcher
import cn.codethink.xiaoming.util.Store
import cn.codethink.xiaoming.util.SegmentId
import cn.codethink.xiaoming.util.SegmentIdMatcher
import cn.codethink.xiaoming.util.StringId
import cn.codethink.xiaoming.util.StringMatcher
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Template
import cn.codethink.xiaoming.util.TestSubjectDescriptor
import cn.codethink.xiaoming.util.TextualId
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.TypeMeta
import cn.codethink.xiaoming.util.UniversalUniqueId
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionMatcher
import cn.codethink.xiaoming.util.WildCardStringMatcher
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
    fun parseTextualId(string: String): TextualId

    fun toStringId(id: Id): StringId
    fun toNumericalId(id: Id): NumericalId
    fun toNamespaceId(id: Id): NamespaceId
    fun toSegmentId(id: Id): SegmentId

    fun createSegmentId(segments: List<String>): SegmentId
    fun parseSegmentId(string: String): SegmentId

    fun createNumericalId(value: Long): NumericalId
    fun createNumericalId(value: Int): NumericalId

    fun createStringId(string: String): StringId

    fun createNamespaceId(group: SegmentId, name: String): NamespaceId
    fun createNamespaceId(group: SegmentId, name: SegmentId): NamespaceId
    fun parseNamespaceId(string: String): NamespaceId

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
    fun parsePluginRequirement(string: String): PluginRequirement

    fun createPluginRequirement(
        id: NamespaceId,
        version: VersionMatcher?,
        channel: StringMatcher?,
        optional: Boolean,
        local: Boolean
    ): PluginRequirement

    // StringMatcher
    fun parseStringMatcher(string: String): StringMatcher
    fun createLiteralStringMatcher(string: String): StringMatcher
    fun createRegexStringMatcher(regex: String): StringMatcher
    fun createWildcardStringMatcher(majority: Boolean, optional: Boolean): WildCardStringMatcher

    // SegmentIdMatcher
    fun parseSegmentIdMatcher(string: String): SegmentIdMatcher
    fun createSegmentIdMatcher(matchers: List<StringMatcher>): SegmentIdMatcher

    // NamespaceIdMatcher
    fun createNamespaceIdMatcher(group: SegmentIdMatcher, name: SegmentIdMatcher): NamespaceIdMatcher
    fun parseNamespaceIdMatcher(string: String): NamespaceIdMatcher

    // PluginSubjectDescriptorMatcher
    fun createPluginSubjectDescriptorMatcher(id: NamespaceIdMatcher): PluginSubjectDescriptorMatcher

    // VersionMatcher
    fun parseVersionMatcher(string: String): VersionMatcher

    fun createAndVersionMatcher(left: VersionMatcher, right: VersionMatcher): AndVersionMatcher
    fun createOrVersionMatcher(left: VersionMatcher, right: VersionMatcher): OrVersionMatcher
    fun createIncludeVersionMatcher(value: Version): IncludeVersionMatcher
    fun createExcludeVersionMatcher(value: Version): ExcludeVersionMatcher
    fun createGreaterThanVersionMatcher(version: Version): GreaterThanVersionMatcher
    fun createGreaterThanOrEqualVersionMatcher(version: Version): GreaterThanOrEqualVersionMatcher
    fun createLessThanVersionMatcher(version: Version): LessThanVersionMatcher
    fun createLessThanOrEqualVersionMatcher(version: Version): LessThanOrEqualVersionMatcher
    fun createMajorVersionPrefixMatcher(major: Int): MajorVersionPrefixMatcher
    fun createMajorMinorVersionPrefixMatcher(major: Int, minor: Int): MajorMinorVersionPrefixMatcher

    // Version
    fun createVersion(major: Int, minor: Int, patch: Int, preRelease: String?, build: String?): Version
    fun parseVersion(string: String): Version

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
    fun findAndApplyInitializers(
        resolver: CodecResolver,
        operation: Operation,
        classLoader: ClassLoader?,
        replace: Boolean,
        visible: Boolean
    )
}