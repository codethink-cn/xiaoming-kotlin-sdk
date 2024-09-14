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

import cn.codethink.xiaoming.common.DATABASE_DATA_SOURCE_TYPE_MYSQL
import cn.codethink.xiaoming.common.LOCAL_PERMISSION_SERVICE_CONFIGURATION_TYPE_DATABASE
import cn.codethink.xiaoming.io.data.CurrentJacksonModuleVersion
import cn.codethink.xiaoming.io.data.DatabaseDataSource
import cn.codethink.xiaoming.io.data.MySqlDatabaseDataSource
import cn.codethink.xiaoming.io.data.polymorphic
import cn.codethink.xiaoming.io.data.subType
import cn.codethink.xiaoming.permission.data.DatabaseLocalPermissionServiceData
import cn.codethink.xiaoming.permission.data.LocalPermissionServiceData
import com.fasterxml.jackson.databind.module.SimpleModule

class LocalPlatformModule : SimpleModule(
    "LocalPlatformModule", CurrentJacksonModuleVersion
) {
    inner class Deserializers {
        val permissionServiceData = polymorphic<LocalPermissionServiceData> {
            subType<DatabaseLocalPermissionServiceData>(LOCAL_PERMISSION_SERVICE_CONFIGURATION_TYPE_DATABASE)
        }
        val databaseDataSource = polymorphic<DatabaseDataSource> {
            subType<MySqlDatabaseDataSource>(DATABASE_DATA_SOURCE_TYPE_MYSQL)
        }
    }

    val deserializers: Deserializers = Deserializers()
}