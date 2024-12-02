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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.util.Cause

interface PluginAllocatingContext {
    val platform: Platform
    val cause: Cause
    val runtimeMeta: PluginRuntimeMeta
}

/**
 * 只加载了插件元数据 [meta] 但尚未加载和分配其他任何资源的插件。
 *
 * 平台会在需要时调用 [allocate] 来分配插件。
 *
 * @author Chuanwise
 */
interface NotYetAllocatedPlugin : Plugin {
    fun allocate(context: PluginAllocatingContext): AllocatedPlugin
}