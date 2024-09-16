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

package cn.codethink.xiaoming.internal.module

import cn.codethink.xiaoming.common.LOCAL_PLATFORM_DATA_TYPE_SQL
import cn.codethink.xiaoming.common.SqlLocalDataModuleSubject
import cn.codethink.xiaoming.io.SqlLocalPlatformDataModule
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformData

class SqlLocalDataModule : Module {
    override val subject = SqlLocalDataModuleSubject
    val jacksonModule = SqlLocalPlatformDataModule()

    override fun onPlatformStart(context: ModuleContext) {
        context.internalApi.serializationApi.registerJacksonModule(jacksonModule, subject)
        context.internalApi.serializationApi.localPlatformModule.deserializers.platformData.registerDeserializer(
            LOCAL_PLATFORM_DATA_TYPE_SQL, jacksonModule.deserializers.sqlLocalPlatformData, subject
        )
    }

    override fun onPlatformStarted(context: ModuleContext) {
        val data = context.internalApi.platformConfiguration.data
        if (data is SqlLocalPlatformData) {
            data.objectMapper = context.internalApi.serializationApi.dataObjectMapper
        }
    }
}