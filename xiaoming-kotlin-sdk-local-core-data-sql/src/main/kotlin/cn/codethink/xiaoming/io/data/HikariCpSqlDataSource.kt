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
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.getValue
import cn.codethink.xiaoming.common.setValue
import com.fasterxml.jackson.annotation.JsonTypeName
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.Properties
import javax.sql.DataSource

const val SQL_DATA_SOURCE_TYPE_HIKARI_CP = "hikari_cp"

/**
 * SQL data source that use HikariCP to manage connections.
 *
 * @author Chuanwise
 * @see SqlDataSource
 */
@JsonTypeName(SQL_DATA_SOURCE_TYPE_HIKARI_CP)
class HikariCpSqlDataSource : AbstractData, SqlDataSource {
    override var type: String by raw
    var properties: Properties by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        properties: Properties,
        raw: Raw = MapRaw()
    ) : super(raw) {
        this.type = SQL_DATA_SOURCE_TYPE_HIKARI_CP
        this.properties = properties
    }

    override fun toDataSource(): DataSource = HikariDataSource(
        HikariConfig(properties)
    )
}