/*
 * Copyright 2025 CodeThink Technologies and contributors.
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

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.data.sql.SqlPlatformData
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.map
import org.ktorm.dsl.neq
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * 主体表，只存主体的 ID 和类型。
 *
 * @property data 数据源
 * @author Chuanwise
 */
class SqlSubjectTable(
    private val data: SqlPlatformData
) : Table<Nothing>(data.tableNamePrefix + "subject") {
    private val id = int("id").primaryKey()
    private val type = varchar("type")
    private val remove = boolean("remove")

    fun allocate(type: String): Id {
        return data.database.insert(this) {
            set(this@SqlSubjectTable.type, type)
        }.toNumericalId()
    }

    fun remove(id: Id) {
        id as NumericalId
        data.database.update(this) {
            set(remove, true)
            where { this@SqlSubjectTable.id eq id.toInt() }
        }
    }

    operator fun get(id: Id): String? {
        id as NumericalId
        return data.database.from(this)
            .select()
            .where((remove neq true) and (this.id eq id.toInt()))
            .map { it[type] }
            .singleOrNull()
    }
}