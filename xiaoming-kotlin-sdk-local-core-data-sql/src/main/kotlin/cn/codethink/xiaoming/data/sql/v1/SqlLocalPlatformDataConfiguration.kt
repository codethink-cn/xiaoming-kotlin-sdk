/*
 * Copyright 2025 CodeThink Technologies and contributors.
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

package cn.codethink.xiaoming.data.sql.v1

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.data.PlatformData
import cn.codethink.xiaoming.data.sql.SqlDataSource
import cn.codethink.xiaoming.data.sql.SqlPlatformDataConfiguration
import cn.codethink.xiaoming.data.sql.SqlPlatformDataImpl
import org.ktorm.database.Database

class SqlPlatformDataConfigurationV1(
    override val source: SqlDataSource,
    override val tableNamePrefix: String
) : SqlPlatformDataConfiguration {
    override fun toData(platform: LocalPlatform): PlatformData {
        return SqlPlatformDataImpl(platform, tableNamePrefix, Database.connect(source.toDataSource()))
    }
}
