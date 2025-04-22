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
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.getOrFail
import cn.codethink.xiaoming.util.toNamespaceIdMatcher
import cn.codethink.xiaoming.util.toNumericalId
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.BaseTable
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar

@OptIn(InternalApi::class)
class SqlPermissionMatcherColumns(
    private val data: SqlPlatformData,
    table: BaseTable<*>,
    columnNamePrefix: String = "matcher_"
) {
    private val matcherType = table.varchar("${columnNamePrefix}type")
    private val matcherInheritedId = table.int("${columnNamePrefix}inherited_id")
    private val matcherWildCardId = table.varchar("${columnNamePrefix}wild_card_id")
    private val matcherWildCardValue = table.boolean("${columnNamePrefix}wild_card_value")

    fun getPermissionMatcher(row: QueryRowSet): PermissionMatcher = with(row) {
        when (val type = getOrFail(matcherType)) {
            InheritancePermissionMatcher.TYPE -> InheritancePermissionMatcher(
                inheritedId = getOrFail(matcherInheritedId).toNumericalId()
            )

            WildCardPermissionPattern.TYPE -> WildCardPermissionMatcher(
                id = getOrFail(matcherWildCardId).toNamespaceIdMatcher(),
                value = get(matcherWildCardValue)
            )

            else -> throw IllegalArgumentException("Unknown matcher type: $type")
        }
    }

    fun setPermissionMatcher(builder: AssignmentsBuilder, matcher: PermissionMatcher) = with(builder) {
        when (matcher) {
            is InheritancePermissionMatcher -> {
                set(matcherType, InheritancePermissionMatcher.TYPE)
                set(matcherInheritedId, matcher.inheritedId.toNumericalId().toInt())
            }

            is WildCardPermissionPattern -> {
                set(matcherType, InheritancePermissionMatcher.TYPE)
                set(matcherWildCardId, matcher.id.toString())
                set(matcherWildCardValue, matcher.value)
            }
        }
    }
}