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

package cn.codethink.xiaoming.permission.data.sql.v1

import cn.codethink.xiaoming.common.SQL_LOCAL_PLATFORM_DATA_VERSION_1
import cn.codethink.xiaoming.common.data.SqlSubjects
import cn.codethink.xiaoming.common.data.SubjectServiceManager
import cn.codethink.xiaoming.common.data.Subjects
import cn.codethink.xiaoming.io.data.SqlDataSource
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformData
import cn.codethink.xiaoming.permission.data.sql.SqlPermissionProfiles
import cn.codethink.xiaoming.permission.data.sql.SqlPermissionRecords
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.ktorm.database.Database

/**
 * @see SqlLocalPlatformData
 */
@JsonIgnoreProperties(
    "objectMapper", "database", "subjectServiceManager",
    "permissionProfiles", "permissionRecords", "subjects", ignoreUnknown = true
)
data class SqlLocalPlatformDataV1(
    override val tables: SqlLocalPlatformDataTablesV1,
    override val source: SqlDataSource,
) : SqlLocalPlatformData {
    override val version: String = SQL_LOCAL_PLATFORM_DATA_VERSION_1

    override var objectMapper: ObjectMapper? = null
    override val database: Database by lazy { Database.connect(source.toDataSource()) }
    override val subjectServiceManager = SubjectServiceManager(this)

    override val permissionProfiles = SqlPermissionProfiles(this)
    override val permissionRecords = SqlPermissionRecords(this)

    override val subjects: Subjects = SqlSubjects(this)
}
