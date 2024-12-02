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

@file:JvmName("AllocatedPlugins")

package cn.codethink.xiaoming.plugin

/**
 * 已分配插件。
 *
 * @author Chuanwise
 */
interface AllocatedPlugin : Plugin {
    /**
     * 插件运行时元数据。
     */
    val runtimeMeta: PluginRuntimeMeta
}

val AllocatedPlugin.isLoaded: Boolean
    get() = runtimeMeta.isLoaded

val AllocatedPlugin.isErrored: Boolean
    get() = runtimeMeta.isErrored

val AllocatedPlugin.isEnabled: Boolean
    get() = runtimeMeta.isEnabled
