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

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.Subject

data class PermissionCalculatingContext<T : Subject>(
    val api: LocalPermissionServiceApi,
    val subject: T,
    val permission: Permission,
    val context: Map<String, Any?> = emptyMap(),
    val caller: Subject? = null,
    val cause: Cause? = null
)

/**
 * Calculating whether the given subject has the given permission.
 *
 * Developers can add default permission profile logic here.
 *
 * @author Chuanwise
 */
interface PermissionCalculator<T : Subject> {
    suspend fun hasPermission(context: PermissionCalculatingContext<T>): Boolean?
}