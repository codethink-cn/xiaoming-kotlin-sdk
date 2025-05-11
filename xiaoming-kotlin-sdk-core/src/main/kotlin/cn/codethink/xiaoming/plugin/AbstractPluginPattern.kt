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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionPattern

abstract class AbstractPluginPattern(
    override val id: NamespaceId,
    override val version: VersionPattern?
) : PluginPattern {
    override fun matches(meta: PluginMeta): Boolean {
        return matches(meta.id, meta.version)
    }

    override fun matches(plugin: Plugin): Boolean {
        return matches(plugin.meta)
    }

    override fun matches(id: NamespaceId, version: Version): Boolean {
        val pattern = this.version
        return id == this.id && (pattern == null || pattern.matches(version))
    }

    override fun matches(signature: PluginSignature): Boolean {
        return matches(signature.id, signature.version)
    }

    abstract override fun toString(): String
}