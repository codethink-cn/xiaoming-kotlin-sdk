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

package cn.codethink.xiaoming.common

/**
 * The subject of the local data sql module.
 */
val SqlLocalDataModuleSubject = ModuleSubject(
    XiaomingSdkSubject.group,
    "xiaoming-kotlin-sdk-core-local-data-sql",
    XiaomingSdkSubject.version
)

const val LOCAL_PLATFORM_DATA_TYPE_SQL = "sql"
const val SQL_LOCAL_PLATFORM_DATA_FIELD_VERSION = "version"
const val SQL_LOCAL_PLATFORM_DATA_VERSION_1 = "1"

const val SQL_DATA_SOURCE_FIELD_TYPE = "type"

const val SQL_DATA_SOURCE_TYPE_HIKARI_CP = "hikari_cp"
const val MYSQL_DATABASE_DATA_SOURCE_FIELD_PROPERTIES = "properties"
