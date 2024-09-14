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

package cn.codethink.xiaoming.permission.data

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_SOURCE
import cn.codethink.xiaoming.common.DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_TABLES
import cn.codethink.xiaoming.common.DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_TABLES_NAME_FIELD_PERMISSION_SUBJECT
import cn.codethink.xiaoming.common.DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_TABLES_NAME_FIELD_PREFIX
import cn.codethink.xiaoming.common.DEFAULT_PERMISSION_SUBJECT_TABLE_NAME
import cn.codethink.xiaoming.common.LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_TYPE
import cn.codethink.xiaoming.common.LOCAL_PERMISSION_SERVICE_CONFIGURATION_TYPE_DATABASE
import cn.codethink.xiaoming.io.data.DatabaseDataSource
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.RawValue
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set
import org.ktorm.database.Database

class DatabaseLocalPermissionServiceData(
    raw: Raw
) : AbstractData(raw), LocalPermissionServiceData {
    override val type: String = "database"
    val source: DatabaseDataSource by raw
    val database: Database by lazy { Database.connect(source.toDataSource()) }

    class Tables(
        raw: Raw
    ) : AbstractData(raw) {
        @RawValue(DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_TABLES_NAME_FIELD_PREFIX)
        val prefix: String by raw

        @RawValue(DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_TABLES_NAME_FIELD_PERMISSION_SUBJECT)
        val permissionSubject: String by raw

        @JvmOverloads
        constructor(
            prefix: String = "",
            permissionSubject: String = DEFAULT_PERMISSION_SUBJECT_TABLE_NAME,
            raw: Raw = MapRaw()
        ) : this(raw) {
            raw[DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_TABLES_NAME_FIELD_PREFIX] = prefix
            raw[DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_TABLES_NAME_FIELD_PERMISSION_SUBJECT] =
                permissionSubject
        }
    }

    val tables: Tables by raw

    override val permissionSubjects by lazy {
        DatabasePermissionSubjects(tables.prefix + tables.permissionSubject, database)
    }

    @JvmOverloads
    constructor(
        tables: Tables,
        source: DatabaseDataSource,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_TYPE] = LOCAL_PERMISSION_SERVICE_CONFIGURATION_TYPE_DATABASE
        raw[DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_SOURCE] = source
        raw[DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_TABLES] = tables
    }
}