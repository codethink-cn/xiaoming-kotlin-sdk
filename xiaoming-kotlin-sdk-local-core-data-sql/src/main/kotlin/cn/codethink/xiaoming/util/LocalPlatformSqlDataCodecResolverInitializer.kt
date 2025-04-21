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

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.data.PlatformDataConfiguration
import cn.codethink.xiaoming.data.sql.HikariCpSqlDataSource
import cn.codethink.xiaoming.data.sql.SqlDataSource
import cn.codethink.xiaoming.data.sql.SqlPlatformDataConfiguration
import cn.codethink.xiaoming.data.sql.v1.SqlPlatformDataConfigurationV1
import cn.codethink.xiaoming.serialization.CodecResolverInitializer
import cn.codethink.xiaoming.serialization.name
import cn.codethink.xiaoming.serialization.names

class LocalPlatformSqlDataCodecResolverInitializer : CodecResolverInitializer {
    override fun initialize(context: SerializationHandlerManagerInitializeContext) {
        context.deserializers.registering(context.operation) {
            names<PlatformDataConfiguration>(FIELD_TYPE) {
                names<SqlPlatformDataConfiguration>(FIELD_VERSION) {
                    name<SqlPlatformDataConfigurationV1>()
                }
            }
            names<SqlDataSource>(FIELD_TYPE) {
                name<HikariCpSqlDataSource>()
            }
        }
    }
}