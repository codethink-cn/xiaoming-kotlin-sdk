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
import cn.codethink.xiaoming.permission.PermissionComparator
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.PermissionRecord
import cn.codethink.xiaoming.permission.data.PermissionRecordData
import org.ktorm.dsl.batchUpdate
import org.ktorm.dsl.eq
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.update
import org.ktorm.entity.Entity
import org.ktorm.entity.filter
import org.ktorm.entity.filterNot
import org.ktorm.entity.map
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.long
import org.ktorm.schema.text

interface SqlPermissionRecord : Entity<SqlPermissionRecord>, PermissionRecord {
    val id: Long
    val remove: Boolean
    val comparatorString: String
    val contextMatcherString: String
    val profileId: Long
}

class SqlPermissionRecordTable(
    data: SqlLocalPlatformData
) : Table<SqlPermissionRecord>(
    data.tables.prefix + data.tables.names.permissionRecords
) {
    val id = long("id").primaryKey().bindTo { it.id }
    val comparator = text("comparator").bindTo { it.comparatorString }
    val contextMatchers = text("context_matchers").bindTo { it.contextMatcherString }

    val profileId = long("profile_id").bindTo { it.profileId }
    val remove = boolean("remove").bindTo { it.remove }
}

@Suppress("UNCHECKED_CAST")
class SqlPermissionRecordData(
    private val data: SqlLocalPlatformData
) : PermissionRecordData {
    val table = SqlPermissionRecordTable(data)

    override fun getRecordsByProfileId(profileId: Long, reverse: Boolean): List<PermissionRecord> {
        return data.database.sequenceOf(table)
            .filterNot { it.remove }
            .filter {
                it.profileId eq profileId
            }.map {
                it["profile"] = data.permissionProfileData.getProfileById(it.profileId)!!
                it["comparator"] =
                    data.objectMapperOrFail.readValue(it.comparatorString, PermissionComparator::class.java)
                it["context"] = data.objectMapperOrFail.readValue(it.contextMatcherString, Map::class.java)
                it
            }.toList().let {
                if (reverse) it.asReversed() else it
            }
    }

    override fun update(record: PermissionRecord) {
        record as SqlPermissionRecord
        data.database.update(table) {
            set(it.profileId, record.profile.id as Long)
            set(it.comparator, data.objectMapperOrFail.writeValueAsString(record.comparator))
            set(it.contextMatchers, data.objectMapperOrFail.writeValueAsString(record.contextMatchers))

            where { it.id eq record.id }
        }
    }

    override fun delete(record: PermissionRecord) {
        record as SqlPermissionRecord
        data.database.update(table) {
            set(it.remove, true)

            where { it.id eq record.id }
        }
    }

    override fun delete(records: List<PermissionRecord>) {
        records as List<SqlPermissionRecord>
        data.database.batchUpdate(table) {
            records.forEach { record ->
                item {
                    set(it.remove, true)
                    where { it.id eq record.id }
                }
            }
        }
    }

    override fun insert(
        profile: PermissionProfile,
        comparator: PermissionComparator,
        contextMatchers: Map<String, Matcher<Any?>>
    ): Long {
        return data.database.insertAndGenerateKey(table) {
            set(it.profileId, profile.id as Long)
            set(it.comparator, data.objectMapperOrFail.writeValueAsString(comparator))
            set(it.contextMatchers, data.objectMapperOrFail.writeValueAsString(contextMatchers))
        } as Long
    }
}