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

/**
 * 插件源：表示一个类似于插件中心的对象。
 *
 * 在启动插件时，若有依赖未满足，将会诉诸插件源以尝试安装新插件。插件源应当自行保证插件的安全性。
 *
 * @author Chuanwise
 */
interface PluginSource {
    /**
     * 获取一个插件所有可用版本。
     *
     * @param id 插件 ID
     * @return 插件可用版本列表
     */
    suspend fun getPluginAvailableVersions(id: NamespaceId): List<PluginAvailableVersion>

    /**
     * 获取能够提供某种插件服务的所有插件列表，不包含插件本身。
     *
     * @param requirement 插件需求
     */
    suspend fun getProviderPlugins(requirement: PluginDependency): List<PluginAvailableVersion>
}