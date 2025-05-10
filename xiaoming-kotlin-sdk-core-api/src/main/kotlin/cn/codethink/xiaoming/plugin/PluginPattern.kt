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

/**
 * 插件需求。
 *
 * @author Chuanwise
 */
interface PluginPattern {
    /**
     * 插件 ID。
     */
    val id: NamespaceId

    /**
     * 插件版本。
     */
    val version: VersionPattern?

    fun matches(meta: PluginMeta): Boolean

    fun matches(plugin: Plugin): Boolean

    fun matches(id: NamespaceId, version: Version): Boolean
}