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
import cn.codethink.xiaoming.common.DATABASE_DATA_SOURCE_TYPE_MYSQL
import cn.codethink.xiaoming.common.MYSQL_DATABASE_DATA_SOURCE_FIELD_PASSWORD
import cn.codethink.xiaoming.common.MYSQL_DATABASE_DATA_SOURCE_FIELD_URL
import cn.codethink.xiaoming.common.Password
import cn.codethink.xiaoming.io.data.set
import com.mysql.cj.jdbc.MysqlDataSource
import javax.sql.DataSource

class MySqlDatabaseDataSource(
    raw: Raw
) : AbstractData(raw), DatabaseDataSource {
    override val type: String = DATABASE_DATA_SOURCE_TYPE_MYSQL

    val url: String by raw
    val password: Password? by raw

    @JvmOverloads
    constructor(
        url: String = "",
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[MYSQL_DATABASE_DATA_SOURCE_FIELD_URL] = url
        raw[MYSQL_DATABASE_DATA_SOURCE_FIELD_PASSWORD] = password
    }

    override fun toDataSource(): DataSource = MysqlDataSource().let {
        it.setURL(url)
        if (password != null) {
            it.password = password!!.toStringUnsafe()
        }
        it
    }
}