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
    override val context: Map<String, Matcher<Any?>>
) : PermissionRecord {
    override val profile: PermissionProfile by lazy { api.getPermissionProfileOrFail(profileId) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SqlPermissionRecord

        if (id != other.id) return false
        if (profileId != other.profileId) return false
        if (comparator != other.comparator) return false
        if (context != other.context) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + profileId.hashCode()
        result = 31 * result + comparator.hashCode()
        result = 31 * result + context.hashCode()
        return result
    }

    override fun toString(): String {
        return "SqlPermissionRecord(id=$id, profileId=$profileId, comparator=$comparator, context=$context)"
    }
}