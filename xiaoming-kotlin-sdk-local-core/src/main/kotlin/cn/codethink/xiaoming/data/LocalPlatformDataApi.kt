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

package cn.codethink.xiaoming.data

import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.permission.PermissionComparator
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.PermissionRecord

/**
 * Operations to access and modify the data of the platform.
 *
 * @author Chuanwise
 */
interface LocalPlatformDataApi {
    // Subject.
    fun getSubjectDescriptor(id: Id): SubjectDescriptor?
    fun getSubjectId(subject: SubjectDescriptor): Id?
    fun getOrInsertSubjectId(subject: SubjectDescriptor): Id

    // Permission Profile.
    fun getPermissionProfiles(): List<PermissionProfile>
    fun getPermissionProfiles(subject: SubjectDescriptor): List<PermissionProfile>

    fun getPermissionProfile(id: Id): PermissionProfile?

    fun insertAndGetPermissionProfileId(subject: SubjectDescriptor): Id

    // Permission Record.
    fun getPermissionRecordsByPermissionProfileId(id: Id, reverse: Boolean = true): List<PermissionRecord>
    fun getPermissionRecordsByPermissionProfile(
        profile: PermissionProfile,
        reverse: Boolean = true
    ): List<PermissionRecord>

    fun deletePermissionRecord(record: PermissionRecord)
    fun deletePermissionRecords(records: List<PermissionRecord>)

    fun insertPermissionRecord(
        profile: PermissionProfile,
        comparator: PermissionComparator,
        context: Map<String, Matcher<Any?>> = emptyMap()
    ): Id
}

fun LocalPlatformDataApi.insertAndGetPermissionProfile(subject: SubjectDescriptor): PermissionProfile =
    getPermissionProfileOrFail(
        insertAndGetPermissionProfileId(subject)
    )

fun LocalPlatformDataApi.getSubjectDescriptorOrFail(id: Id) = getSubjectDescriptor(id)
    ?: throw NoSuchElementException("No subject descriptor found by id: $id.")

fun LocalPlatformDataApi.getPermissionProfileOrFail(id: Id) = getPermissionProfile(id)
    ?: throw NoSuchElementException("No permission profile found by id: $id.")