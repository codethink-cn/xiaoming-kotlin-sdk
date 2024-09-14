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

import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.Subject

/**
 * Operations related to [Permission] and [PermissionMeta].
 *
 * @author Chuanwise
 */
interface PermissionService {
    /**
     * Test if the subject has the permission.
     *
     * @return `null` if the permission is not found.
     */
    suspend fun hasPermission(subject: Subject, permissionMatcher: Matcher<Permission>): Boolean?
}

suspend fun PermissionService.hasPermission(subject: Subject, permission: Permission): Boolean? {
    return hasPermission(subject, permission.toLiteralMatcher())
}
