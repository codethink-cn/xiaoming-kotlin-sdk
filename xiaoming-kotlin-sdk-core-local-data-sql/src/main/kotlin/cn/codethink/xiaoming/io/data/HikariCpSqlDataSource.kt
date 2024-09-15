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

package cn.codethink.xiaoming.io.data

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.MYSQL_DATABASE_DATA_SOURCE_FIELD_PROPERTIES
import cn.codethink.xiaoming.common.SQL_DATA_SOURCE_FIELD_TYPE
import cn.codethink.xiaoming.common.SQL_DATA_SOURCE_TYPE_HIKARI_CP
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.Properties
import javax.sql.DataSource

/**
 * SQL data source that use HikariCP to manage connections.
 *
 * @author Chuanwise
 * @see SqlDataSource
 */
class HikariCpSqlDataSource(
    raw: Raw
) : AbstractData(raw), SqlDataSource {
    override val type: String by raw

    val properties: Properties by raw

    @JvmOverloads
    constructor(
        properties: Properties,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[SQL_DATA_SOURCE_FIELD_TYPE] = SQL_DATA_SOURCE_TYPE_HIKARI_CP
        raw[MYSQL_DATABASE_DATA_SOURCE_FIELD_PROPERTIES] = properties
    }

    override fun toDataSource(): DataSource = HikariDataSource(
        HikariConfig(properties)
    )
}