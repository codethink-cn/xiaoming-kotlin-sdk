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
import cn.codethink.xiaoming.common.DEFAULT_PERMISSION_PROFILE_TABLE_NAME
import cn.codethink.xiaoming.common.DEFAULT_PERMISSION_RECORD_TABLE_NAME
import cn.codethink.xiaoming.common.DEFAULT_TABLE_NAME_PREFIX
import cn.codethink.xiaoming.common.LOCAL_PLATFORM_CONFIGURATION_FIELD_TYPE
import cn.codethink.xiaoming.common.LOCAL_PLATFORM_DATA_TYPE_SQL
import cn.codethink.xiaoming.common.LocalDataSqlModuleSubject
import cn.codethink.xiaoming.common.SQL_LOCAL_PLATFORM_DATA_FIELD_SOURCE
import cn.codethink.xiaoming.common.SQL_LOCAL_PLATFORM_DATA_FIELD_TABLES
import cn.codethink.xiaoming.common.SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PERMISSION_PROFILE
import cn.codethink.xiaoming.common.SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PERMISSION_RECORD
import cn.codethink.xiaoming.common.SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PREFIX
import cn.codethink.xiaoming.io.data.DefaultDataPolymorphicDeserializerService
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.NodeRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.RawValue
import cn.codethink.xiaoming.io.data.SqlDataSource
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set
import com.fasterxml.jackson.databind.ObjectMapper
import org.ktorm.database.Database

class SqlLocalPlatformData(
    raw: Raw
) : AbstractData(raw), LocalPlatformData {
    override val type: String by raw
    
    val source: SqlDataSource by raw
    val database: Database by lazy { Database.connect(source.toDataSource()) }

    var mapper: ObjectMapper? = null

    val mapperOrFail
        get() = mapper ?: throw IllegalStateException("Mapper not set yet.")

    init {
        if (raw is NodeRaw) {
            mapper = raw.mapper
        }
    }

    class Tables(raw: Raw) : AbstractData(raw) {
        val prefix: String by raw

        @RawValue(SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PERMISSION_PROFILE)
        val permissionProfile: String by raw

        @RawValue(SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PERMISSION_RECORD)
        val permissionRecord: String by raw

        @JvmOverloads
        constructor(
            prefix: String = DEFAULT_TABLE_NAME_PREFIX,
            permissionProfile: String = DEFAULT_PERMISSION_PROFILE_TABLE_NAME,
            permissionRecord: String = DEFAULT_PERMISSION_RECORD_TABLE_NAME,
            raw: Raw = MapRaw()
        ) : this(raw) {
            raw[SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PREFIX] = prefix
            raw[SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PERMISSION_PROFILE] =
                permissionProfile
            raw[SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PERMISSION_RECORD] = permissionRecord
        }
    }
    val tables: Tables by raw

    override val permissionProfiles by lazy {
        SqlPermissionProfiles(tables.prefix + tables.permissionProfile, mapper!!, database)
    }
    override val permissionRecords: PermissionRecords by lazy {
        SqlPermissionRecords(tables.prefix + tables.permissionRecord, mapper!!, database)
    }

    @JvmOverloads
    constructor(
        tables: Tables,
        source: SqlDataSource,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[LOCAL_PLATFORM_CONFIGURATION_FIELD_TYPE] = LOCAL_PLATFORM_DATA_TYPE_SQL
        raw[SQL_LOCAL_PLATFORM_DATA_FIELD_SOURCE] = source
        raw[SQL_LOCAL_PLATFORM_DATA_FIELD_TABLES] = tables
    }
}

/**
 * @see DefaultDataPolymorphicDeserializerService
 */
class SqlLocalPlatformPolymorphicDeserializerService : DefaultDataPolymorphicDeserializerService<SqlLocalPlatformData>(
    SqlLocalPlatformData::class.java, LOCAL_PLATFORM_DATA_TYPE_SQL, LocalDataSqlModuleSubject
)