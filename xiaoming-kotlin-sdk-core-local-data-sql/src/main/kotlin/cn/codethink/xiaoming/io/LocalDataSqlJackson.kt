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

package cn.codethink.xiaoming.io

import cn.codethink.xiaoming.common.SQL_DATA_SOURCE_TYPE_HIKARI_CP
import cn.codethink.xiaoming.common.SQL_LOCAL_PLATFORM_DATA_FIELD_VERSION
import cn.codethink.xiaoming.common.SQL_LOCAL_PLATFORM_DATA_VERSION_1
import cn.codethink.xiaoming.io.data.HikariCpSqlDataSource
import cn.codethink.xiaoming.io.data.SqlDataSource
import cn.codethink.xiaoming.io.data.XiaomingJacksonModuleVersion
import cn.codethink.xiaoming.io.data.dataType
import cn.codethink.xiaoming.io.data.polymorphic
import cn.codethink.xiaoming.io.data.subType
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformData
import cn.codethink.xiaoming.permission.data.sql.v1.SqlLocalPlatformDataV1
import com.fasterxml.jackson.databind.module.SimpleModule

class SqlLocalPlatformDataModule : SimpleModule(
    "SqlLocalPlatformDataModule", XiaomingJacksonModuleVersion
) {
    inner class Deserializers {
        val sqlLocalPlatformData = polymorphic<SqlLocalPlatformData>(SQL_LOCAL_PLATFORM_DATA_FIELD_VERSION) {
            subType<SqlLocalPlatformDataV1>(SQL_LOCAL_PLATFORM_DATA_VERSION_1)
        }
        val sqlDataSource = polymorphic<SqlDataSource> {
            dataType<HikariCpSqlDataSource>(SQL_DATA_SOURCE_TYPE_HIKARI_CP)
        }
    }

    val deserializers: Deserializers = Deserializers()
}