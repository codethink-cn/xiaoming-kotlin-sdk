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
import cn.codethink.xiaoming.event.EventContext
import cn.codethink.xiaoming.util.ExperimentalApi
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.PluginDescriptor
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.toPluginDescriptor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Consumer
import kotlin.concurrent.read
import kotlin.concurrent.write

@InternalApi
@OptIn(ExperimentalApi::class)
abstract class AbstractPlugin(
    final override val meta: PluginMeta,

    private val handler: PluginHandler,
    private val manager: LocalPluginManagerImpl,

    initialOperation: Operation,
    initialState: PluginState,

    // 若 platform === manager.platform，表明插件的服务对象是当前宿主。
    // 在插件加载和卸载时，需要获取 manager 内的插件独占锁，以免同 ID 插件冲突。
    // 否则，说明本“插件”只是表示一个正在为远程宿主服务的插件实例，不需要获取这些锁。
    override val platform: Platform,

    private val onLoad: Consumer<AbstractPlugin>,
    private val onUnloadedOrCrashed: Consumer<AbstractPlugin>,
    private val onReleased: Consumer<AbstractPlugin>
) : Plugin {
    internal val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    override val descriptor: PluginDescriptor = meta.id.toPluginDescriptor()

    internal var stateNoLock: PluginState = initialState
    override var state: PluginState
        get() = lock.read { stateNoLock }
        set(value) = lock.write { stateNoLock = value }

    internal var causeNoLock: Operation = initialOperation
    override var operation: Operation
        get() = lock.read { causeNoLock }
        set(value) = lock.write { causeNoLock = value }

    // 如果插件状态正在被改变，则此为导致状态改变的操作，用于另一方打印错误信息。
    internal var transitingCauseNoLock: Operation? = null

    override val isAllocated: Boolean get() = lock.read { stateNoLock >= PluginState.ALLOCATED && stateNoLock < PluginState.RELEASING }
    override val isLoaded: Boolean get() = lock.read { stateNoLock >= PluginState.LOADED && stateNoLock < PluginState.UNLOADING }
    override val isEnabled: Boolean get() = state == PluginState.ENABLED
    override val isCrashed: Boolean get() = state == PluginState.CRASHED

    override val provisions: MutableMap<NamespaceId, Version> = ConcurrentHashMap()

    private fun onLoad() {
        onLoad.accept(this)
    }

    private fun onUnloadedOrCrashed() {
        onUnloadedOrCrashed.accept(this)
    }

    private fun onReleased() {
        onReleased.accept(this)
    }

    private abstract inner class AbstractPluginContext : PluginContext {
        override val platform: Platform = this@AbstractPlugin.platform
        override val plugin: Plugin = this@AbstractPlugin

        override suspend fun crash(operation: Operation) {
            this@AbstractPlugin.crash(operation)
        }

        override suspend fun ensureCrashed(operation: Operation) {
            this@AbstractPlugin.ensureCrashed(operation)
        }
    }

    private inner class PluginAllocateContextImpl(override val event: EventContext<PluginAllocateEvent>) : PluginAllocateContext, AbstractPluginContext()

    private inner class PluginLoadContextImpl(override val event: EventContext<PluginLoadEvent>) : PluginLoadContext, AbstractPluginContext()

    private inner class PluginEnableContextImpl(override val event: EventContext<PluginEnableEvent>) : PluginEnableContext, AbstractPluginContext() {
        override val provisions: MutableMap<NamespaceId, Version> = this@AbstractPlugin.provisions
    }

    private inner class PluginDisableContextImpl(override val event: EventContext<PluginDisableEvent>) : PluginDisableContext, AbstractPluginContext()

    private inner class PluginUnloadContextImpl(override val event: EventContext<PluginUnloadEvent>) : PluginUnloadContext, AbstractPluginContext()

    private inner class PluginReleaseContextImpl(override val event: EventContext<PluginReleaseEvent>) : PluginReleaseContext, AbstractPluginContext()

    override suspend fun allocate(operation: Operation) {
        PluginStateTransition.Allocated.transit(this, operation) {
            handler.onAllocate(PluginAllocateContextImpl(it))
        }
    }

    override suspend fun ensureAllocated(operation: Operation) {
        if (!isAllocated) {
            allocate(operation)
        }
    }

    private fun checkDependencies(operation: Operation, block: suspend (AbstractPlugin) -> Unit) {
        for (dependency in meta.dependencies) {
//            manager.
        }
    }

    override suspend fun load(operation: Operation) {
        onLoad()
        PluginStateTransition.Loaded.transit(this, operation) {
            handler.onLoad(PluginLoadContextImpl(it))
        }
    }

    override suspend fun ensureLoaded(operation: Operation) {
        ensureAllocated(operation)
        if (!isLoaded) {
            load(operation)
        }
    }

    override suspend fun enable(operation: Operation) {
        PluginStateTransition.Enabled.transit(this, operation) {
            handler.onEnable(PluginEnableContextImpl(it))
        }
    }

    override suspend fun ensureEnabled(operation: Operation) {
        ensureLoaded(operation)
        if (!isEnabled) {
            enable(operation)
        }
    }

    override suspend fun disable(operation: Operation) {
        PluginStateTransition.Disabled.transit(this, operation) {
            handler.onDisable(PluginDisableContextImpl(it))
        }
    }

    override suspend fun ensureDisabled(operation: Operation) {
        if (isEnabled) {
            disable(operation)
        }
    }

    override suspend fun unload(operation: Operation) {
        PluginStateTransition.Unloaded.transit(this, operation) {
            handler.onUnload(PluginUnloadContextImpl(it))
        }
        onUnloadedOrCrashed()
    }

    override suspend fun ensureUnloaded(operation: Operation) {
        ensureDisabled(operation)
        if (isLoaded) {
            unload(operation)
        }
    }

    override suspend fun release(operation: Operation) {
        PluginStateTransition.Released.transit(this, operation) {
            handler.onRelease(PluginReleaseContextImpl(it))
        }
        onReleased()
    }

    override suspend fun ensureReleased(operation: Operation) {
        ensureUnloaded(operation)
        if (isAllocated) {
            release(operation)
        }
    }

    internal suspend fun crash(operation: Operation) {
        state = PluginState.CRASHED
        onUnloadedOrCrashed()

        release(operation)
    }

    internal suspend fun ensureCrashed(operation: Operation) {
        if (!isCrashed) {
            crash(operation)
        }
    }
}