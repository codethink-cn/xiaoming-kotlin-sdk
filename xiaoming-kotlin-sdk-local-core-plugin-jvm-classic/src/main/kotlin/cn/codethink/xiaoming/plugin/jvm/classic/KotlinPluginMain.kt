/*
 * Copyright 2024 CodeThink Technologies and contributors.
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

import cn.codethink.xiaoming.plugin.PluginDisableContext
import cn.codethink.xiaoming.plugin.PluginEnableContext
import cn.codethink.xiaoming.plugin.PluginLoadContext
import cn.codethink.xiaoming.plugin.PluginUnloadContext
import cn.codethink.xiaoming.util.InternalApi
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Kotlin 插件主类。
 *
 * @author Chuanwise
 */
class KotlinPluginMain : CoroutineScope {
    private lateinit var mutableCoroutineContext: CoroutineContext
    override val coroutineContext: CoroutineContext get() = mutableCoroutineContext

    suspend fun onLoad(context: PluginLoadContext) = Unit
    suspend fun onEnable(context: PluginEnableContext) = Unit
    suspend fun onDisable(context: PluginDisableContext) = Unit
    suspend fun onUnload(context: PluginUnloadContext) = Unit

    @InternalApi
    internal fun initialize(coroutineContext: CoroutineContext) {
        mutableCoroutineContext = coroutineContext
    }
}
