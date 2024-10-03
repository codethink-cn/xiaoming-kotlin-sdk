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

import cn.codethink.xiaoming.common.FIELD_TYPE
import cn.codethink.xiaoming.common.FIELD_VERSION
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.data.LocalPlatformDataConfiguration
import cn.codethink.xiaoming.io.data.HikariCpSqlDataSource
import cn.codethink.xiaoming.io.data.PolymorphicDeserializerInitializer
import cn.codethink.xiaoming.io.data.PolymorphicDeserializers
import cn.codethink.xiaoming.io.data.SqlDataSource
import cn.codethink.xiaoming.io.data.name
import cn.codethink.xiaoming.io.data.names
import cn.codethink.xiaoming.io.data.subject
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformDataConfiguration
import cn.codethink.xiaoming.permission.data.sql.v1.SqlLocalPlatformDataConfigurationV1

class LocalPlatformSqlDataPolymorphicDeserializerInitializer : PolymorphicDeserializerInitializer {
    override fun initialize(deserializers: PolymorphicDeserializers, subject: Subject) {
        deserializers.subject(subject) {
            names<LocalPlatformDataConfiguration>(FIELD_TYPE) {
                names<SqlLocalPlatformDataConfiguration>(FIELD_VERSION) {
                    name<SqlLocalPlatformDataConfigurationV1>()
                }
            }
            names<SqlDataSource>(FIELD_TYPE) {
                name<HikariCpSqlDataSource>()
            }
        }
    }
}