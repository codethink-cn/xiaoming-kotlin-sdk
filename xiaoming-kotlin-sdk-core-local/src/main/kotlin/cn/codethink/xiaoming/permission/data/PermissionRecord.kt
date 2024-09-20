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

package cn.codethink.xiaoming.permission.data

import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.permission.PermissionComparator

/**
 * Represent a [PermissionProfile] has or has not a permission.
 *
 * @author Chuanwise
 */
interface PermissionRecord {
    val profile: PermissionProfile
    val comparator: PermissionComparator
    val contextMatchers: Map<String, Matcher<Any?>>
}

interface PermissionRecordData {
    fun getRecordsByProfileId(profileId: Long, reverse: Boolean = true): List<PermissionRecord>
    fun getRecordsByProfile(
        profile: PermissionProfile, reverse: Boolean = true
    ): List<PermissionRecord> = getRecordsByProfileId(profile.id, reverse)

    fun update(record: PermissionRecord)

    fun delete(record: PermissionRecord)

    fun delete(records: List<PermissionRecord>)

    fun insert(
        profile: PermissionProfile,
        comparator: PermissionComparator,
        contextMatchers: Map<String, Matcher<Any?>> = emptyMap()
    ): Any
}