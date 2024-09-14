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

const val LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_DATA = "data"
const val LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_TYPE = TYPE_FIELD_NAME

const val LOCAL_PERMISSION_SERVICE_CONFIGURATION_TYPE_DATABASE = "database"
const val DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_SOURCE = "source"
const val DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_TABLES = "tables"
const val DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_TABLES_NAME_FIELD_PREFIX = "prefix"
const val DATABASE_LOCAL_PERMISSION_SERVICE_CONFIGURATION_TABLES_NAME_FIELD_PERMISSION_PROFILE = "permission_profile"

const val DEFAULT_TABLE_NAME_PREFIX = ""
const val DEFAULT_PERMISSION_PROFILE_TABLE_NAME = "permission_profile"

const val DATABASE_DATA_SOURCE_FIELD_TYPE = TYPE_FIELD_NAME

const val DATABASE_DATA_SOURCE_TYPE_MYSQL = "mysql"
const val MYSQL_DATABASE_DATA_SOURCE_FIELD_URL = "url"
const val MYSQL_DATABASE_DATA_SOURCE_FIELD_PASSWORD = "password"
