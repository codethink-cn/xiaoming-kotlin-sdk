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

import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.util.ExperimentalApi
import cn.codethink.xiaoming.util.NotStableForInheritance
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.PluginDescriptor
import cn.codethink.xiaoming.util.Subject
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 插件是一些功能的集合，以对平台产生影响。
 *
 * @author Chuanwise
 */
@NotStableForInheritance
interface Plugin : Subject {
    /**
     * 插件描述符。
     */
    override val descriptor: PluginDescriptor

    /**
     * 插件元数据。
     */
    val meta: PluginMeta

    /**
     * 插件状态。
     */
    val state: PluginState

    /**
     * 导致插件进入当前状态的原因。
     */
    val operation: Operation

    /**
     * 插件服务的宿主。
     */
    val platform: Platform

    /**
     * 插件提供的其他插件功能列表，与 [PluginMeta.provisions] 一一对应。`null` 表示不需要提供。
     */
    val provisions: List<PluginSignature?>

    /**
     * 与 [PluginMeta.dependencies] 一一对应，`null` 表示对应条目未满足。
     */
    val dependencies: List<Plugin?>

    /**
     * 插件是否被分配。
     */
    val isAllocated: Boolean

    /**
     * 插件是否正在分配或已分配。
     */
    val isAllocatingOrAllocated: Boolean

    /**
     * 插件是否被加载。
     */
    val isLoaded: Boolean

    /**
     * 插件是否正在加载或已加载。
     */
    val isLoadingOrLoaded: Boolean

    /**
     * 插件是否被启用。
     */
    val isEnabled: Boolean

    /**
     * 插件是否正在启用或已启用。
     */
    val isEnablingOrEnabled: Boolean

    /**
     * 插件是否已崩溃。
     */
    val isCrashed: Boolean

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun allocate(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun ensureAllocated(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun load(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun ensureLoaded(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun enable(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun ensureEnabled(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun disable(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun ensureDisabled(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun unload(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun ensureUnloaded(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun release(operation: Operation)

    @ExperimentalApi
    @JvmBlockingBridge
    suspend fun ensureReleased(operation: Operation)
}