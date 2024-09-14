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
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.long

interface PermissionSubject {
    val id: Long
    val subject: Subject
}

interface PermissionSubjectFromDatabase : Entity<PermissionSubjectFromDatabase>, PermissionSubject

interface PermissionSubjects {
    fun getPermissionSubjectById(id: Long): PermissionSubject?
}

class DatabasePermissionSubjectTable(
    tableName: String
) : Table<PermissionSubjectFromDatabase>(tableName) {
    val id = long("id").primaryKey().bindTo { it.id }
//    val subject = varchar("subject").bindTo { it.subject.toString() }
}

class DatabasePermissionSubjects(
    tableName: String,
    val database: Database
) : PermissionSubjects {
    val table = DatabasePermissionSubjectTable(tableName)

    override fun getPermissionSubjectById(id: Long): PermissionSubject? {
        return database.sequenceOf(table).firstOrNull { it.id eq id }
    }
}