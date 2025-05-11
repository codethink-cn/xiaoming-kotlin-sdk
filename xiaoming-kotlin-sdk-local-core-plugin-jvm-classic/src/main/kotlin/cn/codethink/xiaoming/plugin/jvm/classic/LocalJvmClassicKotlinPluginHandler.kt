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

import cn.codethink.xiaoming.plugin.PluginAllocateContext
import cn.codethink.xiaoming.plugin.PluginDisableContext
import cn.codethink.xiaoming.plugin.PluginEnableContext
import cn.codethink.xiaoming.plugin.PluginExitContext
import cn.codethink.xiaoming.plugin.PluginLoadContext
import cn.codethink.xiaoming.plugin.PluginUnloadContext

class LocalJvmClassicKotlinPluginHandler(
    override val classPath: LocalJvmClassicPluginClassPath
) : LocalJvmClassicPluginHandler {
    private lateinit var main: KotlinPluginMain

    override suspend fun onAllocate(context: PluginAllocateContext) {
        TODO("Not yet implemented")
    }

    override suspend fun onLoad(context: PluginLoadContext) = main.onLoad(context)
    override suspend fun onEnable(context: PluginEnableContext) = main.onEnable(context)
    override suspend fun onDisable(context: PluginDisableContext) = main.onDisable(context)
    override suspend fun onUnload(context: PluginUnloadContext) = main.onUnload(context)

    override suspend fun onExit(context: PluginExitContext) {
        TODO("Not yet implemented")
    }
}