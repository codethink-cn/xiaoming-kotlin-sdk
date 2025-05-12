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

package cn.codethink.xiaoming.data.sql

import cn.codethink.xiaoming.permission.PermissionBundle
import cn.codethink.xiaoming.permission.PermissionConstraint
import cn.codethink.xiaoming.permission.PermissionEntry
import cn.codethink.xiaoming.permission.PermissionMatcher
import cn.codethink.xiaoming.permission.SqlPermissionBundleTable
import cn.codethink.xiaoming.permission.SqlPermissionEntryTable
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.MutableMapRegistration
import cn.codethink.xiaoming.util.MutableMapRegistrationManagerImpl
import cn.codethink.xiaoming.util.NumericalId
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.SqlSubjectTable
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.format
import cn.codethink.xiaoming.util.toTemplate
import com.fasterxml.jackson.databind.ObjectMapper
import org.ktorm.database.Database

class SqlPlatformDataImpl(
    override val tableNamePrefix: String,
    override val database: Database,
    override val objectMapper: ObjectMapper,
    private val createSchema: Boolean
) : SqlPlatformData {
    private val subjectDescriptorHandlers = MutableMapRegistrationManagerImpl<String, SqlSubjectHandler>()

    override fun registerSubjectDescriptorHandler(
        type: String,
        handler: SqlSubjectHandler,
        operation: Operation
    ): MutableMapRegistration<String, SqlSubjectHandler> {
        return subjectDescriptorHandlers.register(type, handler, operation)
    }

    // Actual subject info stored by the corresponding handler.
    private val subjectTable = SqlSubjectTable(this)
    private val permissionBundleTable = SqlPermissionBundleTable(this)
    private val permissionEntryTable = SqlPermissionEntryTable(this)

    init {
        if (createSchema) {
            val createSchemaSqlTemplatePath = "/xiaoming/data/sql/create_schema.sql"
            val createSchemaSqlTemplate = javaClass
                .getResourceAsStream(createSchemaSqlTemplatePath)
                ?.readBytes()?.decodeToString()?.toTemplate()
                ?: throw NoSuchElementException("Resource not found: $createSchemaSqlTemplatePath")

            val createSchemaSql = createSchemaSqlTemplate.format("table_name_prefix" to tableNamePrefix)
            database.useConnection { it.createStatement().execute(createSchemaSql) }
        }
    }

    override fun getSubjectById(id: Id): SubjectDescriptor? {
        id as NumericalId

        val subjectType = subjectTable[id]
        requireNotNull(subjectType) { "Subject type not found for id: $id" }

        val handler = subjectDescriptorHandlers.getElement(subjectType)
        requireNotNull(handler) { "No handler found for subject type: $subjectType" }

        return handler.getSubjectDescriptorById(id)
    }

    override fun getSubjectId(subject: SubjectDescriptor): Id? {
        val handler = subjectDescriptorHandlers.getElement(subject.type)
        requireNotNull(handler) { "No handler found for subject type: ${subject.type}" }
        return handler.getSubjectId(subject)
    }

    override fun getOrInsertSubjectId(subject: SubjectDescriptor): Id {
        val handler = subjectDescriptorHandlers.getElement(subject.type)
        requireNotNull(handler) { "No handler found for subject type: ${subject.type}" }
        return handler.getOrCreateSubjectDescriptor(subject)
    }

    override fun insertSubjectId(type: String): Id {
        return subjectTable.allocate(type)
    }

    override fun getPermissionBundles(): List<PermissionBundle> {
        return permissionBundleTable.getAll()
    }

    override fun getPermissionBundleById(id: Id): PermissionBundle? {
        return permissionBundleTable.getById(id)
    }

    override fun getPermissionBundles(subjectId: Id): List<PermissionBundle> {
        return permissionBundleTable.getBySubjectId(subjectId)
    }

    override fun insertPermissionBundle(subjectId: Id): Id {
        return permissionBundleTable.insert(subjectId)
    }

    override fun getPermissionEntriesByPermissionBundleId(id: Id, reverse: Boolean): List<PermissionEntry> {
        return permissionEntryTable.getByBundleId(id).let {
            if (reverse) {
                it.sortedByDescending { entry -> entry.operation.time }
            } else {
                it.sortedBy { entry -> entry.operation.time }
            }
        }
    }

    override fun removePermissionEntryById(entryId: Id): Int {
        return permissionEntryTable.remove(entryId)
    }

    override fun addPermissionEntry(
        bundleId: Id,
        matcher: PermissionMatcher,
        constraints: Map<String, PermissionConstraint>,
        operation: Operation
    ): Id {
        return permissionEntryTable.insert(bundleId, matcher, constraints, operation)
    }
}