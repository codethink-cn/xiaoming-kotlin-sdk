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

import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformData
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.map
import org.ktorm.dsl.not
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * Manage plugin subject data in SQL database.
 *
 * @author Chuanwise
 * @see SubjectService
 */
class PluginSubjectService(
    private val data: SqlLocalPlatformData
) : SubjectService<PluginSubject> {
    inner class SubjectTable : Table<Nothing>(
        "${data.tables.prefix}plugin_${data.tables.names.subjects}"
    ) {
        val id = long("id").primaryKey()
        val pluginId = varchar("plugin_id")
        val remove = boolean("remove")
    }

    private val table = SubjectTable()

    override fun getSubjectId(subject: PluginSubject): Long? {
        val pluginId = subject.id
        return data.database.from(table).select(table.id)
            .where { table.pluginId eq pluginId.toString() }
            .where { table.remove.not() }
            .map { it[table.id] }
            .firstOrNull()
    }

    override fun getOrCreateSubjectId(subject: PluginSubject): Long {
        return getSubjectId(subject) ?: run {
            val subjectId = data.subjects.insertAndGetSubjectId(subject.type)
            data.database.insertAndGenerateKey(table) {
                set(it.id, subjectId)
                set(it.pluginId, subject.id.toString())
            }
            subjectId
        }
    }
}