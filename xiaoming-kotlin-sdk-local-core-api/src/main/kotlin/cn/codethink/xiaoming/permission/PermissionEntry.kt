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

import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.TextualId
import cn.codethink.xiaoming.util.Operation

/**
 * 代表一个给某个 [PermissionBundle] 设置权限的记录。
 *
 * @author Chuanwise
 */
interface PermissionEntry {
    /**
     * 设置权限记录的 ID。
     */
    val id: Id

    /**
     * 目标实体。
     */
    val bundleId: Id

    /**
     * 权限匹配器，在此处进行继承之类的检查。
     */
    val matcher: PermissionMatcher

    /**
     * 权限约束，用于评估权限判定的结果是否成立。
     */
    val constraints: Map<String, PermissionConstraint>

    /**
     * 设置权限的跟踪记录。
     */
    val operation: Operation
}