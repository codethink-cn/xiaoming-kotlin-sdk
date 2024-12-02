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


@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.permission.data.sql.v1

import cn.codethink.xiaoming.LocalPlatformApi
import cn.codethink.xiaoming.util.AbstractData
import cn.codethink.xiaoming.util.DefaultValue
import cn.codethink.xiaoming.util.FIELD_TYPE
import cn.codethink.xiaoming.util.FIELD_VERSION
import cn.codethink.xiaoming.util.Field
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.getValue
import cn.codethink.xiaoming.util.setValue
import cn.codethink.xiaoming.data.LocalPlatformDataApi
import cn.codethink.xiaoming.util.MapRaw
import cn.codethink.xiaoming.util.Raw
import cn.codethink.xiaoming.io.data.SqlDataSource
import cn.codethink.xiaoming.io.data.set
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformDataApi
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformDataConfiguration
import com.fasterxml.jackson.annotation.JsonTypeName

const val LOCAL_PLATFORM_DATA_CONFIGURATION_TYPE_SQL = "sql"
const val SQL_LOCAL_PLATFORM_DATA_CONFIGURATION_VERSION_1 = "1"

/**
 * @see SqlLocalPlatformDataConfiguration
 */
@JsonTypeName(SQL_LOCAL_PLATFORM_DATA_CONFIGURATION_VERSION_1)
class SqlLocalPlatformDataConfigurationV1 : AbstractData, SqlLocalPlatformDataConfiguration {
    override val type: String by raw
    override val version: String by raw

    @Field(defaultValue = DefaultValue.EMPTY)
    override var tables: SqlLocalPlatformDataConfigurationTablesV1 by raw
    override var source: SqlDataSource by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        source: SqlDataSource,
        tables: SqlLocalPlatformDataConfigurationTablesV1,
        raw: Raw = MapRaw()
    ) : super(raw) {
        raw[FIELD_TYPE] = LOCAL_PLATFORM_DATA_CONFIGURATION_TYPE_SQL
        raw[FIELD_VERSION] = SQL_LOCAL_PLATFORM_DATA_CONFIGURATION_VERSION_1

        this.source = source
        this.tables = tables
    }

    override fun toDataApi(platformApi: LocalPlatformApi): LocalPlatformDataApi {
        return SqlLocalPlatformDataApi(platformApi.dataObjectMapper, this)
    }
}
