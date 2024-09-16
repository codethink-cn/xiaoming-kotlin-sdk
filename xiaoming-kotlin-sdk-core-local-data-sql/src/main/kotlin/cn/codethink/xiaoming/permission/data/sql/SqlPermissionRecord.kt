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

package cn.codethink.xiaoming.permission.data.sql

import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.PermissionRecord
import cn.codethink.xiaoming.permission.data.PermissionRecords
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.entity.Entity
import org.ktorm.entity.filter
import org.ktorm.entity.filterNot
import org.ktorm.entity.map
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.long
import org.ktorm.schema.text

interface SqlPermissionRecord : Entity<SqlPermissionRecord>, PermissionRecord {
    val id: Long
    val remove: Boolean
    val subjectMatcherString: String
    val nodeMatcherString: String
    val argumentMatchersString: String
    val contextString: String
    val profileId: Long
}

class SqlPermissionRecordTable(
    data: SqlLocalPlatformData
) : Table<SqlPermissionRecord>(
    data.tables.prefix + data.tables.names.permissionRecords
) {
    val id = long("id").primaryKey().bindTo { it.id }
    val subjectMatcher = text("subject_matcher").bindTo { it.subjectMatcherString }

    val nodeMatcher = text("node_matcher").bindTo { it.nodeMatcherString }
    val argumentMatchers = text("argument_matchers").bindTo { it.argumentMatchersString }
    val contextVariables = text("context").bindTo { it.contextString }

    val profileId = long("profile_id").bindTo { it.profileId }
    val value = boolean("value").bindTo { it.value }
    val remove = boolean("remove").bindTo { it.remove }
}

class SqlPermissionRecords(
    private val data: SqlLocalPlatformData
) : PermissionRecords {
    val table = SqlPermissionRecordTable(data)

    override fun getRecords(profile: PermissionProfile, reverse: Boolean): List<PermissionRecord> {
        return data.database.sequenceOf(table)
            .filterNot { it.remove }
            .filter {
                it.profileId eq profile.id
            }.map {
                it["subjectMatcher"] = data.objectMapperOrFail.readValue(it.subjectMatcherString, Matcher::class.java)
                it["nodeMatcher"] = data.objectMapperOrFail.readValue(it.nodeMatcherString, Matcher::class.java)
                it["argumentMatchers"] = data.objectMapperOrFail.readValue(it.argumentMatchersString, Map::class.java)
                it["context"] = data.objectMapperOrFail.readValue(it.contextString, Map::class.java)
                it
            }.toList().let {
                if (reverse) it.asReversed() else it
            }
    }

    override fun addRecord(
        profile: PermissionProfile,
        subjectMatcher: Matcher<Subject>,
        nodeMatcher: Matcher<SegmentId>,
        value: Boolean?,
        argumentMatchers: Map<String, Matcher<*>>,
        context: Map<String, Any?>
    ) {
        data.database.insert(table) {
            set(it.profileId, profile.id)
            set(it.subjectMatcher, data.objectMapperOrFail.writeValueAsString(subjectMatcher))
            set(it.nodeMatcher, data.objectMapperOrFail.writeValueAsString(nodeMatcher))
            set(it.value, value)
            set(it.argumentMatchers, data.objectMapperOrFail.writeValueAsString(argumentMatchers))
            set(it.contextVariables, data.objectMapperOrFail.writeValueAsString(context))
        }
    }
}