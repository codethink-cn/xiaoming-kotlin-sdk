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

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.plugin.PluginDependency
import cn.codethink.xiaoming.plugin.PluginProvision
import cn.codethink.xiaoming.plugin.PluginSignature
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionPattern

class JvmClassicPluginMetaV1(
    override val id: NamespaceId,
    override val name: String,
    override val main: String,
    override val version: Version,
    override val description: String? = null,
    override val standard: VersionPattern? = null,
    override val provisions: List<PluginProvision> = emptyList(),
    override val dependencies: List<PluginDependency> = emptyList(),
) : JvmClassicPluginMeta {
    override val signature: PluginSignature = PluginSignature(id, version)
}