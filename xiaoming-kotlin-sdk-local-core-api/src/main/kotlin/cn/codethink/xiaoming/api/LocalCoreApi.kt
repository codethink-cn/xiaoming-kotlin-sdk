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
import cn.codethink.xiaoming.permission.WildCardPermissionPattern
import cn.codethink.xiaoming.plugin.PluginConfiguration
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceIdPattern

/**
 * 本地核心 API，是通过 local-core-api 模块主动调用 local-core 的桥梁。
 *
 * @author Chuanwise
 */
@InternalApi
interface LocalCoreApi {
    companion object {
        @JvmStatic
        fun getInstance(): LocalCoreApi = LocalCoreApiInstance.get()
    }

    // PermissionMatchers
    fun createWildCardPermissionMatcher(id: NamespaceIdPattern, value: Boolean?): WildCardPermissionPattern
    fun createInheritancePermissionMatcher(inheritedId: Id): InheritancePermissionMatcher

    fun createPluginConfiguration(sharable: Boolean, debug: Boolean, crashOnRemoved: Boolean): PluginConfiguration
}