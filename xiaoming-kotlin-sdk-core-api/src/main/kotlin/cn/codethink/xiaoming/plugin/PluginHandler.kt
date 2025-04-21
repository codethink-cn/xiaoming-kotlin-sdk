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

/**
 * 插件主类，实现插件的加载、启动、关闭和卸载操作。
 *
 * @author Chuanwise
 */
interface PluginHandler {
    suspend fun onLoad(context: PluginLoadContext)
    suspend fun onEnable(context: PluginEnableContext)
    suspend fun onDisable(context: PluginDisableContext)
    suspend fun onUnload(context: PluginUnloadContext)
}