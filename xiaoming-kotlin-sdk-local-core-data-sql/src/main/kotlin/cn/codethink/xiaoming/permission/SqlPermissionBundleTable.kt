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

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.data.sql.SqlPlatformData
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NumericalId
import cn.codethink.xiaoming.util.getOrFail
import cn.codethink.xiaoming.util.toNumericalId
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.ktorm.entity.filter
import org.ktorm.entity.filterNot
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.singleOrNull
import org.ktorm.entity.toList
import org.ktorm.schema.BaseTable
import org.ktorm.schema.boolean
import org.ktorm.schema.int

@OptIn(InternalApi::class)
class SqlPermissionBundleTable(
    private val data: SqlPlatformData
) : BaseTable<PermissionBundle>(data.tableNamePrefix + "permission_bundle") {
    private val id = int("id").primaryKey()
    private val subjectId = int("subject_id")
    private val remove = boolean("remove")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): PermissionBundle = with(row) {
        PermissionBundleImpl(
            id = getOrFail(id).toNumericalId(),
            subjectId = getOrFail(subjectId).toNumericalId()
        )
    }

    fun insert(subjectId: Id): Id {
        subjectId as NumericalId
        return data.database.insert(this) {
            set(this@SqlPermissionBundleTable.subjectId, subjectId.toInt())
        }.toNumericalId()
    }

    fun remove(id: Id) {
        id as NumericalId
        data.database.update(this) {
            set(remove, true)
            where { it.id eq id.toInt() }
        }
    }

    fun getAll(): List<PermissionBundle> {
        return data.database.sequenceOf(this)
            .filterNot { it.remove }
            .toList()
    }

    fun getById(id: Id): PermissionBundle? {
        id as NumericalId
        return data.database.sequenceOf(this)
            .filterNot { it.remove }
            .filter { it.id eq id.toInt() }
            .singleOrNull()
    }

    fun getBySubjectId(subjectId: Id): List<PermissionBundle> {
        subjectId as NumericalId
        return data.database.sequenceOf(this)
            .filterNot { it.remove }
            .filter { it.subjectId eq subjectId.toInt() }
            .toList()
    }
}