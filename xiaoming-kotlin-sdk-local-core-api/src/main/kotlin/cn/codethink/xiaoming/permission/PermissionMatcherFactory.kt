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

@file:JvmName("PermissionMatcherFactory")

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.api.LocalCoreApi
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceIdPattern

@JvmOverloads
@OptIn(InternalApi::class)
@JvmName("createWildCardPermissionMatcher")
fun WildCardPermissionMatcher(id: NamespaceIdPattern, value: Boolean? = true): WildCardPermissionPattern {
    return LocalCoreApi.getInstance().createWildCardPermissionMatcher(id, value)
}

@OptIn(InternalApi::class)
@JvmName("createInheritancePermissionMatcher")
fun InheritancePermissionMatcher(inheritedId: Id): InheritancePermissionMatcher {
    return LocalCoreApi.getInstance().createInheritancePermissionMatcher(inheritedId)
}