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

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.util.DualKeyMap
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Registration
import cn.codethink.xiaoming.util.Version

/**
 * 本地插件管理器。
 *
 * @author Chuanwise
 */
interface LocalPluginManager : PluginManager {
    /**
     * 宿主。
     */
    override val platform: LocalPlatform

    /**
     * 宿主的所有插件，其中包括已识别，但未加载的插件。
     */
    val availablePlugins: DualKeyMap<NamespaceId, Version, Plugin>

    /**
     * 插件源。
     */
    val sources: List<Registration<PluginSource>>

    fun registerPlugin(meta: PluginMeta, mode: PluginMode, allocator: PluginAllocator): Plugin

    fun resolvePlugin(requirement: PluginRequirement): Plugin

    fun tryResolvePlugin()
}