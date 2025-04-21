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

package cn.codethink.xiaoming.data.sql

import cn.codethink.xiaoming.data.PlatformData
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.MutableMapRegistration
import cn.codethink.xiaoming.util.Operation
import com.fasterxml.jackson.databind.ObjectMapper
import org.ktorm.database.Database

interface SqlPlatformData : PlatformData {
    val tableNamePrefix: String
    val objectMapper: ObjectMapper

    val database: Database

    fun registerSubjectDescriptorHandler(
        type: String,
        handler: SqlSubjectHandler,
        operation: Operation
    ): MutableMapRegistration<String, SqlSubjectHandler>

    // 一般只有 SqlSubjectHandler 使用此函数。
    fun insertSubjectId(type: String): Id
}