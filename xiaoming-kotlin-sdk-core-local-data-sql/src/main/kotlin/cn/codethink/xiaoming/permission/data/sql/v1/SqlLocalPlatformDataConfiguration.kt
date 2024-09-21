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

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.FIELD_TYPE
import cn.codethink.xiaoming.common.FIELD_VERSION
import cn.codethink.xiaoming.data.LocalPlatformDataApi
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.SqlDataSource
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set
import cn.codethink.xiaoming.io.data.setValue
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformDataApi
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformDataConfiguration
import com.fasterxml.jackson.annotation.JsonTypeName

const val LOCAL_PLATFORM_DATA_CONFIGURATION_TYPE_SQL = "sql"
const val SQL_LOCAL_PLATFORM_DATA_CONFIGURATION_VERSION_1 = "1"

/**
 * @see SqlLocalPlatformDataConfiguration
 */
@JsonTypeName(SQL_LOCAL_PLATFORM_DATA_CONFIGURATION_VERSION_1)
class SqlLocalPlatformDataConfigurationV1(
    raw: Raw
) : AbstractData(raw), SqlLocalPlatformDataConfiguration {
    override val type: String by raw
    override val version: String by raw

    override var tables: SqlLocalPlatformDataConfigurationTablesV1 by raw
    override var source: SqlDataSource by raw

    @JvmOverloads
    constructor(
        source: SqlDataSource,
        tables: SqlLocalPlatformDataConfigurationTablesV1,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = LOCAL_PLATFORM_DATA_CONFIGURATION_TYPE_SQL
        raw[FIELD_VERSION] = SQL_LOCAL_PLATFORM_DATA_CONFIGURATION_VERSION_1

        this.source = source
        this.tables = tables
    }

    override fun toDataApi(internalApi: LocalPlatformInternalApi): LocalPlatformDataApi {
        return SqlLocalPlatformDataApi(internalApi, this)
    }
}
