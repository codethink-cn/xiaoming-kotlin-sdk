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

import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.NotStableForInheritance

/**
 * 表示一个具体的操作。
 *
 * 尽管使用 [NotStableForInheritance]，但未来可能被解除。在根据需要扩展结构化权限节点后，
 * 可能对实现 [Permission] 的类做特殊要求。因此目前请不要实现此接口。
 *
 * @author Chuanwise
 * @see PermissionManager
 */
@NotStableForInheritance
interface Permission {
    /**
     * 权限 ID。
     */
    val id: NamespaceId
}