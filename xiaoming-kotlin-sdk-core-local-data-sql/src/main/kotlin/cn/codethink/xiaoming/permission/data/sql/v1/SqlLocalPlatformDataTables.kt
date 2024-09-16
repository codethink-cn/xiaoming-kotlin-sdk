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

import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformData

/**
 * @author Chuanwise
 * @see SqlLocalPlatformDataTables
 */
interface SqlLocalPlatformDataTableNames {
    val permissionProfiles: String
    val permissionRecords: String
    val subjects: String
}

data class SqlLocalPlatformDataTableNamesV1(
    override val permissionProfiles: String = "permission_profile",
    override val permissionRecords: String = "permission_record",
    override val subjects: String = "subject"
) : SqlLocalPlatformDataTableNames

/**
 * @author Chuanwise
 * @see SqlLocalPlatformData
 */
interface SqlLocalPlatformDataTables {
    val prefix: String
    val names: SqlLocalPlatformDataTableNames
}

data class SqlLocalPlatformDataTablesV1(
    override val prefix: String = "",
    override val names: SqlLocalPlatformDataTableNamesV1 = SqlLocalPlatformDataTableNamesV1()
) : SqlLocalPlatformDataTables
