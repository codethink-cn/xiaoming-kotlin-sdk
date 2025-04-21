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
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.SqlOperationColumns
import cn.codethink.xiaoming.util.TextualId
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
import org.ktorm.jackson.json
import org.ktorm.schema.BaseTable
import org.ktorm.schema.boolean
import org.ktorm.schema.int

@OptIn(InternalApi::class)
class SqlPermissionEntryTable(
    private val data: SqlPlatformData
) : BaseTable<PermissionEntry>(data.tableNamePrefix + "permission_entry") {
    private val id = int("id").primaryKey()
    private val bundleId = int("bundle_id")
    private val matcherColumns = SqlPermissionMatcherColumns(data, this)
    private val constraints = json<Map<String, PermissionConstraint>>("constraints")
    private val operationColumns = SqlOperationColumns(data, this)
    private val remove = boolean("remove")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): PermissionEntry = with(row) {
        PermissionEntryImpl(
            id = getOrFail(id).toNumericalId(),
            bundleId = getOrFail(bundleId).toNumericalId(),
            matcher = matcherColumns.getPermissionMatcher(row),
            constraints = getOrFail(constraints),
            operation = operationColumns.getOperation(row),
        )
    }

    fun getById(id: Id): PermissionEntry? {
        id as NumericalId
        return data.database.sequenceOf(this)
            .filterNot { remove }
            .filter { it.id eq id.toInt() }
            .singleOrNull()
    }

    fun getByBundleId(bundleId: Id): List<PermissionEntry> {
        bundleId as NumericalId
        return data.database.sequenceOf(this)
            .filterNot { remove }
            .filter { this.bundleId eq bundleId.toInt() }
            .toList()
    }

    fun remove(id: Id): Int {
        id as NumericalId
        return data.database.update(this) {
            set(remove, true)
            where { this@SqlPermissionEntryTable.id eq id.toInt() }
        }
    }

    fun insert(
        bundleId: Id,
        matcher: PermissionMatcher,
        constraints: Map<String, PermissionConstraint>,
        operation: Operation
    ): Id {
        bundleId as NumericalId
        return data.database.insert(this) {
            set(this@SqlPermissionEntryTable.bundleId, bundleId.toInt())
            matcherColumns.setPermissionMatcher(this, matcher)
            set(this@SqlPermissionEntryTable.constraints, constraints)
            operationColumns.setOperation(this, operation)
        }.toNumericalId()
    }
}