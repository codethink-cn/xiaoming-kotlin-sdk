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

import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Version

/**
 * 插件入口，表示一个插件。
 *
 * @author Chuanwise
 */
interface PluginEntry {
    /**
     * 插件元数据。
     */
    val meta: PluginMeta

    /**
     * 插件状态。
     */
    val state: PluginState

    /**
     * 插件模式。
     */
    val mode: PluginMode

    /**
     * 插件服务的宿主。
     */
    val platform: Platform

    /**
     * 插件提供的功能列表。
     */
    val provisions: Map<NamespaceId, Version>

    /**
     * 插件是否被分配。
     */
    val isAllocated: Boolean

    /**
     * 插件是否被加载。
     */
    val isLoaded: Boolean

    /**
     * 插件是否被启用。
     */
    val isEnabled: Boolean
}