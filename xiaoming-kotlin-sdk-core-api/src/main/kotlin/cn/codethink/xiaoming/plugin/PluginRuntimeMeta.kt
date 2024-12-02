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

@file:JvmName("PluginRuntimeMetas")

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Version

/**
 * 插件运行时元数据。
 *
 * @author Chuanwise
 */
interface PluginRuntimeMeta {
    val mode: PluginMode
    val state: PluginState

    val isLoaded: Boolean
    val isLoading: Boolean
    val isLoadingErrored: Boolean
    val isEnabled: Boolean
    val isEnabling: Boolean
    val isEnablingErrored: Boolean
    val isDisabled: Boolean
    val isDisabling: Boolean
    val isDisablingErrored: Boolean
    val isUnloaded: Boolean
    val isUnloading: Boolean
    val isUnloadingErrored: Boolean

    val provisions: Map<NamespaceId, Version>
}

val PluginRuntimeMeta.isLoadedOrLoadingErrored: Boolean
    get() = isLoadingErrored || isLoaded

val PluginRuntimeMeta.isEnabledOrEnablingErrored: Boolean
    get() = isEnablingErrored || isEnabled

val PluginRuntimeMeta.isDisabledOrDisablingErrored: Boolean
    get() = isDisablingErrored || isDisabled

val PluginRuntimeMeta.isUnloadedOrUnloadingErrored: Boolean
    get() = isUnloadingErrored || isUnloaded

val PluginRuntimeMeta.isErrored: Boolean
    get() = isLoadingErrored || isEnablingErrored || isDisablingErrored || isUnloadingErrored

val PluginRuntimeMeta.isProcessing: Boolean
    get() = isLoading || isEnabling || isDisabling || isUnloading

val PluginRuntimeMeta.isNotProcessing: Boolean
    get() = !isProcessing

val PluginRuntimeMeta.isNotError: Boolean
    get() = !isErrored