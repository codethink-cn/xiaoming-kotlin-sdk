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

import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.NumericalId
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.data.LocalPlatformDataApi
import cn.codethink.xiaoming.data.toId
import cn.codethink.xiaoming.permission.PermissionComparator
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.PermissionRecord
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


const val TYPE_VARCHAR_LENGTH = 255

/**
 * @author Chuanwise
 * @see LocalPlatformDataApi
 */
class SqlLocalPlatformDataApi(
    private val objectMapper: ObjectMapper,
    private val configuration: SqlLocalPlatformDataConfiguration
) : LocalPlatformDataApi {

    // Tools to use `internalApi.serializationApi.internalObjectMapper` to serialize and deserialize objects.
    private inline fun <reified T : Any> Table.json(name: String): Column<T> {
        return json(name, { objectMapper.writeValueAsString(it) }, { objectMapper.readValue(it, T::class.java) })
    }

    private val database = Database.connect(configuration.source.toDataSource())

    // Tables.
    private inner class SubjectTable : IntIdTable(
        configuration.tables.prefix + configuration.tables.names.subjects
    ) {
        val type = varchar("type", TYPE_VARCHAR_LENGTH).index()
        val descriptor = json<SubjectDescriptor>("descriptor")
        val remove = bool("remove").default(false).index()
    }

    private val subjects = SubjectTable()

    private inner class PermissionProfileTable : IntIdTable(
        configuration.tables.prefix + configuration.tables.names.permissionProfiles
    ) {
        // Who created this permission profile?
        val subjectId = reference("subject_id", subjects)
        val remove = bool("remove").default(false).index()
    }

    private val permissionProfiles = PermissionProfileTable()
    private fun ResultRow.toPermissionProfile(): PermissionProfile = SqlPermissionProfile(
        api = this@SqlLocalPlatformDataApi,
        id = this[permissionProfiles.id].toId(),
        subjectId = this[permissionProfiles.subjectId].toId()
    )

    private inner class PermissionRecordTable : IntIdTable(
        configuration.tables.prefix + configuration.tables.names.permissionRecords
    ) {
        val profileId = reference("permission_profile_id", permissionProfiles)
        val comparator = json<PermissionComparator>("comparator")
        val context = json<Map<String, Matcher<Any?>>>("context")
        val remove = bool("remove").default(false).index()
    }

    private val permissionRecords = PermissionRecordTable()
    private fun ResultRow.toPermissionRecord(): PermissionRecord = SqlPermissionRecord(
        api = this@SqlLocalPlatformDataApi,
        id = this[permissionRecords.id].toId(),
        profileId = this[permissionRecords.profileId].toId(),
        comparator = this[permissionRecords.comparator],
        context = this[permissionRecords.context]
    )

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                subjects, permissionProfiles, permissionRecords
            )
        }
    }

    override fun getSubjectDescriptor(id: Id): SubjectDescriptor? {
        id as NumericalId
        return transaction(database) {
            subjects.select(subjects.descriptor)
                .where { not(subjects.remove) and (subjects.id eq id.toInt()) }
                .map { it[subjects.descriptor] }
                .singleOrNull()
        }
    }

    override fun getSubjectId(subject: SubjectDescriptor): NumericalId? = transaction(database) {
        subjects.select(subjects.id)
            .where { not(subjects.remove) and (subjects.type eq subject.type) and (subjects.descriptor eq subject) }
            .map { it[subjects.id] }
            .singleOrNull()
            ?.toId()
    }

    override fun getOrInsertSubjectId(subject: SubjectDescriptor): NumericalId = transaction(database) {
        subjects.insertAndGetId {
            it[type] = subject.type
            it[this.descriptor] = subject
        }.toId()
    }

    override fun getPermissionProfiles(): List<PermissionProfile> = transaction(database) {
        permissionProfiles.selectAll()
            .where { not(permissionProfiles.remove) }
            .map { it.toPermissionProfile() }
    }

    override fun getPermissionProfile(id: Id): PermissionProfile? {
        id as NumericalId
        return transaction(database) {
            permissionProfiles.selectAll()
                .where { not(permissionProfiles.remove) and (permissionProfiles.id eq id.toInt()) }
                .map {
                    SqlPermissionProfile(
                        api = this@SqlLocalPlatformDataApi,
                        id = it[permissionProfiles.id].toId(),
                        subjectId = it[permissionProfiles.subjectId].toId()
                    )
                }
                .singleOrNull()
        }
    }

    override fun getPermissionProfiles(subject: SubjectDescriptor): List<PermissionProfile> = transaction(database) {
        permissionProfiles.innerJoin(subjects)
            .selectAll()
            .where {
                not(permissionProfiles.remove) and not(subjects.remove) and (subjects.type eq subject.type) and (subjects.descriptor eq subject)
            }
            .map { it.toPermissionProfile() }
    }

    override fun insertAndGetPermissionProfileId(subject: SubjectDescriptor): Id = transaction(database) {
        permissionProfiles.insertAndGetId {
            it[subjectId] = getOrInsertSubjectId(subject).toInt()
        }.toId()
    }

    override fun getPermissionRecordsByPermissionProfileId(id: Id, reverse: Boolean): List<PermissionRecord> {
        id as NumericalId
        return transaction(database) {
            permissionRecords.selectAll()
                .where { not(permissionRecords.remove) and (permissionRecords.profileId eq id.toInt()) }
                .map { it.toPermissionRecord() }
                .let { if (reverse) it.reversed() else it }
        }
    }

    override fun getPermissionRecordsByPermissionProfile(
        profile: PermissionProfile,
        reverse: Boolean
    ): List<PermissionRecord> = getPermissionRecordsByPermissionProfileId(profile.id, reverse)

    override fun deletePermissionRecord(record: PermissionRecord) {
        record as SqlPermissionRecord
        transaction(database) {
            permissionRecords.update({ permissionRecords.id eq record.id.toInt() }) {
                it[permissionRecords.remove] = true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun deletePermissionRecords(records: List<PermissionRecord>) {
        records as List<SqlPermissionRecord>
        transaction(database) {
            permissionRecords.batchUpsert(records) {
                this[permissionRecords.remove] = true
            }
        }
    }

    override fun insertPermissionRecord(
        profile: PermissionProfile,
        comparator: PermissionComparator,
        context: Map<String, Matcher<Any?>>
    ): NumericalId {
        profile as SqlPermissionProfile
        return transaction(database) {
            permissionRecords.insertAndGetId {
                it[profileId] = profile.id.toInt()
                it[permissionRecords.comparator] = comparator
                it[permissionRecords.context] = context
            }.toId()
        }
    }
}