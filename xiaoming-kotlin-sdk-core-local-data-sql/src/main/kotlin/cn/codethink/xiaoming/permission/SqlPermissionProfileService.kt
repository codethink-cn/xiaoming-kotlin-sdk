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

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.SqlLocalPlatformData
import com.fasterxml.jackson.module.kotlin.readValue
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.filter
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.text

class SqlPermissionProfileService(
    val type: String,
    val data: SqlLocalPlatformData,
    private val subjectProfileIdCache: MutableMap<Subject, Long>
) : PermissionProfileService {
    interface PermissionProfileIdMapping : Entity<PermissionProfileIdMapping> {
        val type: String
        val subject: String
        val profileId: Long
    }

    inner class PermissionProfileIdTable : Table<PermissionProfileIdMapping>(
        "${data.tables.prefix}_permission_profile_mapping"
    ) {
        val type = text("type").bindTo { it.type }
        val subject = text("subject").bindTo { it.subject }
        val profileId = long("profile_id").bindTo { it.profileId }
    }

    private val table = PermissionProfileIdTable()

    private fun getMappings() = data.database.sequenceOf(table).filter {
        it.type eq type
    }.toList()

    override suspend fun getPermissionProfile(
        api: LocalPermissionServiceApi, subject: Subject
    ): PermissionProfile? {
        if (subject.type != type) {
            throw IllegalArgumentException("Subject type not match: ${subject.type} != $type.")
        }

        val profileId = subjectProfileIdCache[subject] ?: getMappings().firstOrNull {
            data.mapperOrFail.readValue<Subject>(it.subject) == subject
        }?.profileId?.apply {
            subjectProfileIdCache[subject] = this
        } ?: return null

        return data.permissionProfiles.getProfileById(profileId)
    }

    override suspend fun hasPermission(
        api: LocalPermissionServiceApi,
        subject: Subject,
        permission: Permission
    ): Boolean? {
        val profile = getPermissionProfile(api, subject) ?: return null
        return api.hasPermission(profile, permission)
    }
}