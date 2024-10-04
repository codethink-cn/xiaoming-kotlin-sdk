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

import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.NumericalId
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.data.getSubjectDescriptorOrFail
import cn.codethink.xiaoming.permission.data.PermissionProfile

data class SqlPermissionProfile(
    private val api: SqlLocalPlatformDataApi,
    override val id: NumericalId,
    private val subjectId: Id
) : PermissionProfile {
    override val subject: SubjectDescriptor by lazy { api.getSubjectDescriptorOrFail(subjectId) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SqlPermissionProfile

        if (id != other.id) return false
        if (subjectId != other.subjectId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + subjectId.hashCode()
        return result
    }

    override fun toString(): String {
        return "SqlPermissionProfile(id=$id, subjectId=$subjectId)"
    }
}