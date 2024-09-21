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

package cn.codethink.xiaoming.permission.data.sql

import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.NumericalId
import cn.codethink.xiaoming.data.getPermissionProfileOrFail
import cn.codethink.xiaoming.permission.PermissionComparator
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.PermissionRecord

class SqlPermissionRecord(
    private val api: SqlLocalPlatformDataApi,
    val id: NumericalId,
    private val profileId: NumericalId,
    override val comparator: PermissionComparator,
    override val contextMatchers: Map<String, Matcher<Any?>>
) : PermissionRecord {
    override val profile: PermissionProfile by lazy { api.getPermissionProfileOrFail(profileId) }
}