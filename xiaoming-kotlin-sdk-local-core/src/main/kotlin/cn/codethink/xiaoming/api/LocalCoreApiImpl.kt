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

package cn.codethink.xiaoming.api

import cn.codethink.xiaoming.permission.InheritancePermissionMatcher
import cn.codethink.xiaoming.permission.InheritancePermissionMatcherV1
import cn.codethink.xiaoming.permission.WildCardPermissionMatcher
import cn.codethink.xiaoming.permission.WildCardPermissionMatcherV1
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceIdMatcher

@InternalApi
class LocalCoreApiImpl : LocalCoreApi {
    // PermissionMatchers
    override fun createWildCardPermissionMatcher(id: NamespaceIdMatcher, value: Boolean?): WildCardPermissionMatcher {
        return WildCardPermissionMatcherV1(id, value)
    }

    override fun createInheritancePermissionMatcher(inheritedId: Id): InheritancePermissionMatcher {
        return InheritancePermissionMatcherV1(inheritedId)
    }
}