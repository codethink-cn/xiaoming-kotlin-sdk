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

@file:JvmName("PermissionServices")
package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.SubjectDescriptor
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 权限管理器，管理和权限相关的请求。
 *
 * @author Chuanwise
 */
interface PermissionManager {
    @JvmBlockingBridge
    suspend fun testPermission(
        target: SubjectDescriptor,
        permission: Permission,
        cause: Cause,
        context: Map<String, Any?> = emptyMap()
    ): Boolean?
}
