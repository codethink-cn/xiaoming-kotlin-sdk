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
interface Plugin : Subject, PluginEntry {
    /**
     * 插件描述符。
     */
    override val descriptor: PluginDescriptor

    @JvmBlockingBridge
    suspend fun load(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT)

    @JvmBlockingBridge
    suspend fun ensureLoaded(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT)

    @JvmBlockingBridge
    suspend fun tryLoad(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT): Boolean

    @JvmBlockingBridge
    suspend fun tryEnsureLoaded(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT): Boolean

    @JvmBlockingBridge
    suspend fun enable(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT)

    @JvmBlockingBridge
    suspend fun ensureEnabled(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT)

    @JvmBlockingBridge
    suspend fun tryEnable(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT): Boolean

    @JvmBlockingBridge
    suspend fun tryEnsureEnabled(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT): Boolean

    @JvmBlockingBridge
    suspend fun disable(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT)

    @JvmBlockingBridge
    suspend fun ensureDisabled(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT)

    @JvmBlockingBridge
    suspend fun tryDisable(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT): Boolean

    @JvmBlockingBridge
    suspend fun tryEnsureDisabled(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT): Boolean

    @JvmBlockingBridge
    suspend fun unload(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT)

    @JvmBlockingBridge
    suspend fun ensureUnloaded(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT)

    @JvmBlockingBridge
    suspend fun tryUnload(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT): Boolean

    @JvmBlockingBridge
    suspend fun tryEnsureUnloaded(operation: Operation, policy: PluginStateChangePolicy = PluginStateChangePolicy.STRICT): Boolean
}