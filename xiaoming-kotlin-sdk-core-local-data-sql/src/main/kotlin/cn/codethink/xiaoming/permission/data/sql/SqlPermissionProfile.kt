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

package cn.codethink.xiaoming.permission.data.sql

import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.PermissionProfileData
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.map
import org.ktorm.dsl.not
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.entity.Entity
import org.ktorm.entity.filterNot
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.long

interface SqlPermissionProfile : Entity<SqlPermissionProfile>, PermissionProfile {
    override val id: Long
    val subjectId: Long
    val remove: Boolean
}

class SqlPermissionProfileTable(
    data: SqlLocalPlatformData
) : Table<SqlPermissionProfile>(
    data.tables.prefix + data.tables.names.permissionProfiles
) {
    val id = long("id").primaryKey().bindTo { it.id }
    val subjectId = long("subject_id").bindTo { it.subjectId }
    val remove = boolean("remove").bindTo { it.remove }
}

@Suppress("UNCHECKED_CAST")
class SqlPermissionProfileData(
    private val data: SqlLocalPlatformData
) : PermissionProfileData {
    val table = SqlPermissionProfileTable(data)

    override fun getProfiles(): List<SqlPermissionProfile> {
        return data.database.sequenceOf(table)
            .filterNot { it.remove }
            .toList()
    }

    override fun getProfileById(id: Long): SqlPermissionProfile? {
        id as Long
        return data.database.sequenceOf(table)
            .filterNot { it.remove }
            .firstOrNull { it.id eq id }
    }

    override fun getIdMatchedProfiles(matcher: Matcher<Long>): List<PermissionProfile> {
        return data.database.sequenceOf(table)
            .filterNot { it.remove }
            .toList()
            .filter { matcher.isMatched(it.id) }
    }

    override fun getIdMatchedProfileIds(matcher: Matcher<Long>): List<Long> {
        return data.database.from(table)
            .select(table.id)
            .where { table.remove.not() }
            .map { it[table.id] }
            .toList()
            .filter { matcher.isMatched(it!!) } as List<Long>
    }

    override fun getOrInsertProfileId(subject: Subject): Long {
        val subjectId = data.subjectServiceManager.getOrCreateSubjectId(subject)
        getProfileById(subjectId)?.let {
            return it.id
        }

        return data.database.insertAndGenerateKey(table) {
            set(it.subjectId, subjectId)
        } as Long
    }

    override fun getOrInsertProfile(subject: Subject): SqlPermissionProfile {
        val subjectId = data.subjectServiceManager.getOrCreateSubjectId(subject)
        getProfileById(subjectId)?.let {
            return it
        }

        val profileId = data.database.insertAndGenerateKey(table) {
            set(it.subjectId, subjectId)
        } as Long

        return getProfileById(profileId)!!
    }
}