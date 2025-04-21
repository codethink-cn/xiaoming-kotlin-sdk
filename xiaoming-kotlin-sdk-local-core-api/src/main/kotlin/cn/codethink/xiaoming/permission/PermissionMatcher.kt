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

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.util.NotStableForInheritance
import cn.codethink.xiaoming.util.Tristate

/**
 * 权限匹配器，存储在权限管理器中，用于比较所需查询的权限 [Permission] 是否在范围内。
 *
 * @author Chuanwise
 * @see InheritancePermissionMatcher
 * @see WildCardPermissionMatcher
 */
@NotStableForInheritance
interface PermissionMatcher {
    val type: String

    /**
     * 尝试匹配权限。
     *
     * @param context 权限检查上下文
     * @return 若所需权限与之无关，则返回 `null`。
     */
    fun matches(context: PermissionMatcherContext): Tristate?
}