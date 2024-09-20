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

import cn.codethink.xiaoming.common.LOCAL_PLATFORM_DATA_TYPE_SQL
import cn.codethink.xiaoming.common.data.SubjectServiceManager
import cn.codethink.xiaoming.common.data.Subjects
import cn.codethink.xiaoming.data.LocalPlatformData
import cn.codethink.xiaoming.io.data.SqlDataSource
import cn.codethink.xiaoming.permission.data.sql.v1.SqlLocalPlatformDataTables
import com.fasterxml.jackson.databind.ObjectMapper
import org.ktorm.database.Database

/**
 * Using databases supporting JDBC to store local platform data, whose type
 * is [LOCAL_PLATFORM_DATA_TYPE_SQL].
 *
 * @author Chuanwise
 * @see LocalPlatformData
 */
interface SqlLocalPlatformData : LocalPlatformData {
    val version: String
    val source: SqlDataSource
    val database: Database
    val tables: SqlLocalPlatformDataTables
    var objectMapper: ObjectMapper?

    val subjects: Subjects
    val subjectServiceManager: SubjectServiceManager

    override val type: String
        get() = LOCAL_PLATFORM_DATA_TYPE_SQL
}

val SqlLocalPlatformData.objectMapperOrFail: ObjectMapper
    get() = objectMapper
        ?: throw IllegalStateException("Object mapper not set yet. Before access data, set object mapper first.")
