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

/**
 * 插件管理器。
 *
 * @author Chuanwise
 */
interface PluginManager {
    /**
     * 插件管理器对应的平台。
     */
    val platform: Platform

    /**
     * 已经加载的插件。
     */
    val plugins: Map<NamespaceId, Plugin>

    /**
     * 已经加载的插件，包含提供者信息。
     */
    val providerPlugins: Map<NamespaceId, Plugin>

    /**
     * 根据插件 ID 获取插件。
     *
     * @param id 插件 ID。
     * @return 插件。
     */
    fun getPlugin(id: NamespaceId): Plugin?

    fun getPluginOrFail(id: NamespaceId): Plugin

    fun getProviderPlugin(id: NamespaceId): Plugin?

    fun getProviderPluginOrFail(id: NamespaceId): Plugin
}