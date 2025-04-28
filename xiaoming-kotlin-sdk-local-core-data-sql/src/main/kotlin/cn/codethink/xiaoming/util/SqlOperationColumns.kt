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
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.QueryRowSet
import org.ktorm.jackson.json
import org.ktorm.schema.BaseTable
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.uuid

@OptIn(InternalApi::class)
class SqlOperationColumns(
    private val data: SqlPlatformData,
    table: BaseTable<*>,
    columnNamePrefix: String = "operation_"
) {
    private val id = table.uuid("${columnNamePrefix}id")
    private val operatorId = table.int("${columnNamePrefix}operator_id")
    private val cause = table.json<Cause>("${columnNamePrefix}cause", data.objectMapper)
    private val time = table.long("${columnNamePrefix}time")
    private val operation = table.json<Operation>("${columnNamePrefix}operation", data.objectMapper)

    fun getOperation(row: QueryRowSet): Operation = with(row) {
        return getOrFail(operation)
    }

    fun setOperation(builder: AssignmentsBuilder, operation: Operation) = with(builder) {
        val uuid = (operation.id as UniversalUniqueId).toUuid()
        set(id, uuid)
        set(operatorId, data.getOrInsertSubjectId(operation.operator).toNumericalId().toInt())
        set(cause, operation.cause)
        set(time, operation.time.toUnixMilliseconds())
        set(this@SqlOperationColumns.operation, operation)
    }
}