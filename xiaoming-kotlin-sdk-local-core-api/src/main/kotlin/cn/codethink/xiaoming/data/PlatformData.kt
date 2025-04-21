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

import cn.codethink.xiaoming.permission.PermissionConstraint
import cn.codethink.xiaoming.permission.PermissionBundle
import cn.codethink.xiaoming.permission.PermissionEntry
import cn.codethink.xiaoming.permission.PermissionMatcher
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Operation

/**
 * 平台数据存储。
 *
 * @author Chuanwise
 */
interface PlatformData {
    // Subject.
    fun getSubjectById(id: Id): SubjectDescriptor?
    fun getSubjectId(subject: SubjectDescriptor): Id?
    fun getOrInsertSubjectId(subject: SubjectDescriptor): Id

    fun getPermissionBundles(): List<PermissionBundle>
    fun getPermissionBundles(subjectId: Id): List<PermissionBundle>
    fun getPermissionBundleById(id: Id): PermissionBundle?
    fun insertPermissionBundle(subjectId: Id): Id

    fun getPermissionEntriesByPermissionBundleId(id: Id, reverse: Boolean = true): List<PermissionEntry>

    fun removePermissionEntryById(entryId: Id): Int

    fun addPermissionEntry(
        bundleId: Id,
        matcher: PermissionMatcher,
        constraints: Map<String, PermissionConstraint>,
        operation: Operation
    ): Id
}