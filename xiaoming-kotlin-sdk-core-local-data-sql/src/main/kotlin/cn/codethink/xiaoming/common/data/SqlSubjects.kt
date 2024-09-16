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

package cn.codethink.xiaoming.common.data

import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformData
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.map
import org.ktorm.dsl.not
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * Manage subjects.
 *
 * @author Chuanwise
 */
interface Subjects {
    fun insertAndGetSubjectId(type: String): Long
    fun getSubjectType(id: Long): String?
    fun deleteSubject(id: Long): Boolean
}


class SqlSubjects(
    private val data: SqlLocalPlatformData
) : Subjects {
    inner class SubjectsTable : Table<Nothing>(
        data.tables.prefix + data.tables.names.subjects
    ) {
        val id = long("id").primaryKey()
        val type = varchar("type")
        val remove = boolean("remove")
    }

    private val table = SubjectsTable()

    override fun insertAndGetSubjectId(type: String): Long {
        return data.database.insertAndGenerateKey(table) {
            set(it.type, type)
        } as Long
    }

    override fun getSubjectType(id: Long): String? {
        return data.database.from(table)
            .select(table.type)
            .where { table.remove.not() }
            .where { table.id eq id }
            .map { it[table.type] }
            .firstOrNull()
    }

    override fun deleteSubject(id: Long): Boolean {
        return data.database.update(table) {
            set(it.remove, true)
            where { it.id eq id }
        } > 0
    }
}

fun SqlSubjects.getSubjectTypeOrFail(id: Long): String = getSubjectType(id)
    ?: throw NoSuchElementException("No subject type found for $id.")



