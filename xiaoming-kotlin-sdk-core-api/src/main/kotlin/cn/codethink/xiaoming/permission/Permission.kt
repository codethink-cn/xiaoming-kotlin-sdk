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

@file:JvmName("Permissions")

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalImplementedApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.SegmentId

/**
 * 表示一个具体的操作。
 *
 * @author Chuanwise
 * @see PermissionManager
 */
@InternalImplementedApi
interface Permission {
    /**
     * 权限 ID。
     *
     * 内置标准权限是普通的 [SegmentId]，第三方扩展的权限 ID 必须使用 [NamespaceId]。
     */
    val id: Id

    /**
     * 权限参数。
     */
    val arguments: Map<String, Any?>

    /**
     * 权限描述符。
     */
    val descriptor: PermissionDescriptor
}

fun Permission.toLiteralMatcher(): PermissionMatcher = createLiteralPermissionMatcher(this)
