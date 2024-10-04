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

/**
 * Manages the runtime meta of a plugin.
 *
 * @author Chuanwise
 */
interface PluginRuntimeMeta {
    val meta: PluginMeta
    val state: PluginState
    val level: PluginLevel

    val isLoaded: Boolean
    val isEnabled: Boolean
    val isError: Boolean
}

val PluginRuntimeMeta.isNotLoaded: Boolean
    get() = !isLoaded

val PluginRuntimeMeta.isNotEnabled: Boolean
    get() = !isEnabled

val PluginRuntimeMeta.isNotError: Boolean
    get() = !isError