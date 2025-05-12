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

import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.plugin.Plugin
import cn.codethink.xiaoming.plugin.PluginDisableContext
import cn.codethink.xiaoming.plugin.PluginEnableContext
import cn.codethink.xiaoming.plugin.PluginLoadContext
import cn.codethink.xiaoming.plugin.PluginUnloadContext
import java.io.File

interface PluginMain {
    /**
     * 主类对应的插件。
     */
    val plugin: Plugin

    /**
     * 插件所服务的宿主。
     */
    val platform: Platform

    /**
     * 当前插件的类路径。
     */
    val classPath: JvmClassicPluginClassPath

    /**
     * 插件目录文件。
     */
    val directoryFile: File

    /**
     * 执行插件加载操作。
     *
     * @param context 插件加载上下文
     */
    suspend fun onLoad(context: PluginLoadContext)

    /**
     * 执行插件启动操作。
     *
     * @param context 插件启动上下文
     */
    suspend fun onEnable(context: PluginEnableContext)

    /**
     * 执行插件关闭操作。
     *
     * @param context 插件关闭上下文
     */
    suspend fun onDisable(context: PluginDisableContext)

    /**
     * 执行插件卸载操作。
     *
     * @param context 插件卸载上下文
     */
    suspend fun onUnload(context: PluginUnloadContext)
}