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

import com.fasterxml.jackson.databind.ObjectMapper
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.filter
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.long
import org.ktorm.schema.text

interface SqlPermissionRecord : Entity<SqlPermissionRecord>, PermissionRecord {
    val id: Long
}

class SqlPermissionRecordTable(
    tableName: String, mapper: ObjectMapper
) : Table<SqlPermissionRecord>(tableName) {
    val id = long("id").primaryKey().bindTo { it.id }
    val profileId = long("profile_id").bindTo { it.profile.id }
    val subjectId = text("subject").bindTo { mapper.writeValueAsString(it.subject) }
    val node = text("node").bindTo { mapper.writeValueAsString(it.node) }
    val context = text("context").bindTo { mapper.writeValueAsString(it.context) }
    val value = boolean("value").bindTo { it.value }
}

class SqlPermissionRecords(
    tableName: String,
    val mapper: ObjectMapper,
    val database: Database
) : PermissionRecords {
    val table = SqlPermissionRecordTable(tableName, mapper)

    override fun getRecords(profile: PermissionProfile): List<PermissionRecord> {
        return database.sequenceOf(table).filter {
            it.profileId eq profile.id
        }.toList()
    }
}