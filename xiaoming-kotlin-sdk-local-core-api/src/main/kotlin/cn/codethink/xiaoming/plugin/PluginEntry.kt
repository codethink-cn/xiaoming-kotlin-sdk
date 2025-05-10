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

interface PluginEntry {
    /**
     * 插件元数据。
     */
    val meta: PluginMeta

    /**
     * 插件配置。
     */
    val configuration: PluginConfiguration

    /**
     * 尝试将插件转换为插件处理器。
     *
     * @return 插件处理器
     */
    suspend fun toPluginHandler(): PluginHandler
}