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
import cn.codethink.xiaoming.permission.WildCardPermissionPattern
import cn.codethink.xiaoming.permission.WildCardPermissionPatternV1
import cn.codethink.xiaoming.plugin.JvmPluginClassAccessPolicyImpl
import cn.codethink.xiaoming.plugin.PluginConfiguration
import cn.codethink.xiaoming.plugin.PluginConfigurationImpl
import cn.codethink.xiaoming.plugin.jvm.JvmPluginClassAccessPolicy
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceIdPattern

@InternalApi
class LocalCoreApiImpl : LocalCoreApi {
    // PermissionMatchers
    override fun createWildCardPermissionMatcher(id: NamespaceIdPattern, value: Boolean?): WildCardPermissionPattern {
        return WildCardPermissionPatternV1(id, value)
    }

    override fun createInheritancePermissionMatcher(inheritedId: Id): InheritancePermissionMatcher {
        return InheritancePermissionMatcherV1(inheritedId)
    }

    override fun createPluginConfiguration(sharable: Boolean, debug: Boolean, crashOnRemoved: Boolean, retainOnConflict: Boolean): PluginConfiguration {
        return PluginConfigurationImpl(sharable, debug, crashOnRemoved, retainOnConflict)
    }

    override fun createJvmPluginClassAccessPolicy(accessible: Boolean): JvmPluginClassAccessPolicy {
        return JvmPluginClassAccessPolicyImpl.of(accessible)
    }
}