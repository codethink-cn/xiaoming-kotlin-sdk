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

import cn.codethink.xiaoming.library.LibraryDescriptor
import cn.codethink.xiaoming.library.LibraryDescriptorImpl
import cn.codethink.xiaoming.permission.InheritancePermissionMatcher
import cn.codethink.xiaoming.permission.InheritancePermissionMatcherV1
import cn.codethink.xiaoming.permission.WildCardPermissionPattern
import cn.codethink.xiaoming.permission.WildCardPermissionPatternV1
import cn.codethink.xiaoming.plugin.PluginConfiguration
import cn.codethink.xiaoming.plugin.PluginConfigurationV1
import cn.codethink.xiaoming.plugin.jvm.JvmPluginClassAccessPolicy
import cn.codethink.xiaoming.plugin.jvm.JvmPluginClassAccessPolicyImpl
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

    override fun createPluginConfiguration(sharable: Boolean, crashOnRemoved: Boolean, retainOnConflict: Boolean): PluginConfiguration {
        return PluginConfigurationV1(sharable, crashOnRemoved, retainOnConflict)
    }

    override fun createJvmPluginClassAccessPolicy(accessible: Boolean): JvmPluginClassAccessPolicy {
        return JvmPluginClassAccessPolicyImpl.of(accessible)
    }

    override fun createLibraryDescriptor(string: String): LibraryDescriptor {
        val colonIndexAfterGroup = string.indexOf(':')
        require(colonIndexAfterGroup != -1) { "Invalid library descriptor: $string" }

        val colonIndexAfterName = string.indexOf(':', colonIndexAfterGroup + 1)
        require(colonIndexAfterName != -1) { "Invalid library descriptor: $string" }

        val group = string.substring(0, colonIndexAfterGroup)
        val name = string.substring(colonIndexAfterGroup + 1, colonIndexAfterName)
        val version = string.substring(colonIndexAfterName + 1)
        return LibraryDescriptorImpl(group, name, version)
    }

    override fun createLibraryDescriptor(group: String, name: String, version: String): LibraryDescriptor {
        return LibraryDescriptorImpl(group, name, version)
    }
}