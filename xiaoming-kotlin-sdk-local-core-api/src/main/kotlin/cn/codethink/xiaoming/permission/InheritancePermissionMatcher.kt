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

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.NotStableForInheritance
import com.fasterxml.jackson.annotation.JsonTypeName

/**
 * 继承权限检查器，用于检查权限是否可被继承自另一权限包。
 *
 * 例如，如果权限包 1 具备权限 `a.b.c` 且权限包 2 具备 `inheritance(set = 1)`，
 * 则权限包 2 也将具备 `a.b.c`。
 *
 * @author Chuanwise
 */
@NotStableForInheritance
@JsonTypeName(InheritancePermissionMatcher.TYPE)
interface InheritancePermissionMatcher : PermissionMatcher {
    companion object {
        const val TYPE = "inheritance"
    }

    override val type: String get() = TYPE

    /**
     * 被继承的权限包 ID。
     */
    val inheritedId: Id
}