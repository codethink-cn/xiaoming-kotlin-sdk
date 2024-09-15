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
val LocalDataSqlModuleSubject = ModuleSubject(
    CurrentSdkSubject.group,
    "xiaoming-kotlin-sdk-core-local",
    CurrentSdkSubject.version
)

const val LOCAL_PLATFORM_DATA_TYPE_SQL = "sql"
const val SQL_LOCAL_PLATFORM_DATA_FIELD_SOURCE = "source"
const val SQL_LOCAL_PLATFORM_DATA_FIELD_TABLES = "tables"
const val SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PREFIX = "prefix"
const val SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PERMISSION_PROFILE = "permission_profile"
const val SQL_LOCAL_PLATFORM_DATA_TABLES_NAME_FIELD_PERMISSION_RECORD = "permission_record"

const val DEFAULT_TABLE_NAME_PREFIX = ""
const val DEFAULT_PERMISSION_PROFILE_TABLE_NAME = "permission_profile"
const val DEFAULT_PERMISSION_RECORD_TABLE_NAME = "permission_record"


const val SQL_DATA_SOURCE_FIELD_TYPE = "type"

const val SQL_DATA_SOURCE_TYPE_HIKARI_CP = "hikari_cp"
const val MYSQL_DATABASE_DATA_SOURCE_FIELD_PROPERTIES = "properties"
