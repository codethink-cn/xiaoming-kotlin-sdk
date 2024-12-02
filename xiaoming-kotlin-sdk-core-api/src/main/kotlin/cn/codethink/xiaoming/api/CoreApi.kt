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

import cn.codethink.xiaoming.permission.Permission
import cn.codethink.xiaoming.permission.PermissionMatcher
import cn.codethink.xiaoming.plugin.PluginRequirement
import cn.codethink.xiaoming.util.AndVersionMatcher
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.Data
import cn.codethink.xiaoming.util.ExcludeVersionMatcher
import cn.codethink.xiaoming.util.GreaterThanOrEqualVersionMatcher
import cn.codethink.xiaoming.util.GreaterThanVersionMatcher
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.IncludeVersionMatcher
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.LessThanOrEqualVersionMatcher
import cn.codethink.xiaoming.util.LessThanVersionMatcher
import cn.codethink.xiaoming.util.MajorMinorVersionPrefixMatcher
import cn.codethink.xiaoming.util.MajorVersionPrefixMatcher
import cn.codethink.xiaoming.util.Matcher
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.NumericalId
import cn.codethink.xiaoming.util.OrVersionMatcher
import cn.codethink.xiaoming.util.PluginSubjectDescriptorMatcher
import cn.codethink.xiaoming.util.Raw
import cn.codethink.xiaoming.util.SegmentId
import cn.codethink.xiaoming.util.SegmentIdMatcher
import cn.codethink.xiaoming.util.StandardCause
import cn.codethink.xiaoming.util.StringId
import cn.codethink.xiaoming.util.StringMatcher
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Template
import cn.codethink.xiaoming.util.TestSubjectDescriptor
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.UniversalUniqueId
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionMatcher
import cn.codethink.xiaoming.util.WildcardStringMatcher
import java.util.UUID

/**
 * 小明 [CoreApi]，是通过 `xiaoming-kotlin-sdk-core-api` 模块主动调用
 * `xiaoming-kotlin-sdk-core` 的桥梁。
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
    fun parseId(string: String): Id

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
    fun parseTemplate(format: String): Template

    // Cause
    fun createCause(message: String, subject: SubjectDescriptor): Cause

    fun createEmptyTextCause(
        id: Id,
        text: String,
        subject: SubjectDescriptor,
        cause: Cause?
    ): StandardCause

    // Subject Descriptor
    fun createTestSubjectDescriptor(): TestSubjectDescriptor

    // Data
    fun createData(raw: Raw): Data

    // Time
    fun createTimeOfMilliseconds(milliseconds: Long): Time

    // PermissionMatchers
    fun createLiteralPermissionMatcher(permission: Permission): PermissionMatcher

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
    fun createWildcardStringMatcher(majority: Boolean, optional: Boolean, count: Int?): WildcardStringMatcher

    // SegmentIdMatcher
    fun parseSegmentIdMatcher(string: String): SegmentIdMatcher
    fun createSegmentIdMatcher(matchers: List<StringMatcher>): SegmentIdMatcher
    fun createSegmentIdMatcher(segmentId: SegmentId): SegmentIdMatcher

    // PluginSubjectDescriptorMatcher
    fun createPluginSubjectDescriptorMatcher(id: Matcher<NamespaceId>): PluginSubjectDescriptorMatcher

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
}