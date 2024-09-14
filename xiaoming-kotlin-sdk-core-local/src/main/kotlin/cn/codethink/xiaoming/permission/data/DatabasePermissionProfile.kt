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

import cn.codethink.xiaoming.common.Subject
import com.fasterxml.jackson.databind.ObjectMapper
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.entity.Entity
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.text

interface DatabasePermissionProfile : Entity<DatabasePermissionProfile>, PermissionProfile

class DatabasePermissionSubjectTable(
    tableName: String, mapper: ObjectMapper
) : Table<DatabasePermissionProfile>(tableName) {
    val id = long("id").primaryKey().bindTo { it.id }
    val subject = text("subject").bindTo { mapper.writeValueAsString(it.subject) }
}

class DatabasePermissionProfiles(
    tableName: String,
    val mapper: ObjectMapper,
    val database: Database
) : PermissionProfiles {
    val table = DatabasePermissionSubjectTable(tableName, mapper)

    override fun getProfileById(id: Long): PermissionProfile? {
        return database.sequenceOf(table).firstOrNull { it.id eq id }
    }

    override fun insertAndGetProfileId(subject: Subject): Long = database.insertAndGenerateKey(table) {
        set(it.subject, mapper.writeValueAsString(subject))
    } as Long
}