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
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.PluginDescriptor
import cn.codethink.xiaoming.util.toPluginDescriptor
import cn.codethink.xiaoming.util.transitiveClosure
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@InternalApi
@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractPlugin(
    final override val meta: PluginMeta,

    val handler: PluginHandler,

    initialOperation: Operation,
    initialState: PluginState,

    // 若 platform === manager.platform，表明插件的服务对象是当前宿主。
    // 在插件加载和卸载时，需要获取 manager 内的插件独占锁，以免同 ID 插件冲突。
    // 否则，说明本“插件”只是表示一个正在为远程宿主服务的插件实例，不需要获取这些锁。
    override val platform: Platform
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
    internal var transitingOperationNoLock: Operation? = null
    internal var transitingOperation: Operation?
        get() = lock.read { transitingOperationNoLock }
        set(value) = lock.write { transitingOperationNoLock = value }

    override val allDependencies: List<Plugin> get() = transitiveClosure(dependencies.filterNotNull()) { it.dependencies.filterNotNull() }

    override val isAllocated: Boolean get() = lock.read { stateNoLock >= PluginState.ALLOCATED && stateNoLock < PluginState.RELEASING }
    override val isAllocatingOrAllocated: Boolean get() = lock.read { stateNoLock >= PluginState.ALLOCATING && stateNoLock < PluginState.RELEASING }

    override val isLoaded: Boolean get() = lock.read { stateNoLock >= PluginState.LOADED && stateNoLock < PluginState.UNLOADING }
    override val isLoadingOrLoaded: Boolean get() = lock.read { stateNoLock >= PluginState.LOADING && stateNoLock < PluginState.UNLOADING }

    override val isEnabled: Boolean get() = state == PluginState.ENABLED
    override val isEnablingOrEnabled: Boolean get() = lock.read { stateNoLock >= PluginState.ENABLING && stateNoLock < PluginState.DISABLING }

    override val isCrashed: Boolean get() = state == PluginState.CRASHED
    override val isCrashingOrCrashed: Boolean get() = lock.read { stateNoLock == PluginState.CRASHING || stateNoLock == PluginState.CRASHED }

    override val isReleased: Boolean get() = state == PluginState.RELEASED
    override val isReleasingOrReleased: Boolean get() = lock.read { stateNoLock >= PluginState.RELEASING && stateNoLock < PluginState.RELEASED }

    override val isExited: Boolean get() = lock.read { stateNoLock == PluginState.RELEASED || stateNoLock == PluginState.CRASHED }
    override val isExitingOrExited: Boolean
        get() = lock.read {
            stateNoLock == PluginState.RELEASING || stateNoLock == PluginState.RELEASED || stateNoLock == PluginState.CRASHING || stateNoLock == PluginState.CRASHED
        }

    // 下面的函数分为四类：action, doAction 和 onDoAction
    // 1. action 即对外暴露的 API，调用可能导致依赖解析、锁获取和动作执行等等。
    // 2. onAction 是对外暴露 API 在动作执行（doAction）之前可以执行的一些操作，通常是依赖检查或解除。
    // 3. doAction 是真正执行操作的函数，其中在操作前或后执行 onDoAction，以便维护内部状态（如锁）。

    protected open suspend fun onDoLoad(operation: Operation) {}

    protected open suspend fun onDoEnable(operation: Operation) {}

    protected open suspend fun onDoDisable(operation: Operation) {}

    protected open suspend fun onDoUnloaded(operation: Operation) {}

    protected open suspend fun onDoCrashed(operation: Operation) {}

    protected open suspend fun onDoReleased(operation: Operation) {}

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

    private inner class PluginAllocateContextImpl(override val eventContext: EventContext<PluginAllocateEvent>) : PluginAllocateContext, AbstractPluginContext()

    private inner class PluginLoadContextImpl(override val eventContext: EventContext<PluginLoadEvent>) : PluginLoadContext, AbstractPluginContext()

    private inner class PluginEnableContextImpl(override val eventContext: EventContext<PluginEnableEvent>) : PluginEnableContext, AbstractPluginContext()

    private inner class PluginDisableContextImpl(override val eventContext: EventContext<PluginDisableEvent>) : PluginDisableContext, AbstractPluginContext()

    private inner class PluginUnloadContextImpl(override val eventContext: EventContext<PluginUnloadEvent>) : PluginUnloadContext, AbstractPluginContext()

    private inner class PluginExitContextImpl(override val eventContext: EventContext<PluginExitEvent>) : PluginExitContext, AbstractPluginContext()

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

    internal suspend fun doLoad(operation: Operation) {
        onDoLoad(operation)
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

    internal suspend fun ensureDoLoad(operation: Operation) {
        ensureAllocated(operation)
        if (!isLoaded) {
            doLoad(operation)
        }
    }

    internal suspend fun doEnable(operation: Operation) {
        onDoEnable(operation)
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

    internal suspend fun ensureDoEnable(operation: Operation) {
        ensureDoLoad(operation)
        if (!isEnabled) {
            doEnable(operation)
        }
    }

    internal suspend fun doDisable(operation: Operation) {
        onDoDisable(operation)
        PluginStateTransition.Disabled.transit(this, operation) {
            handler.onDisable(PluginDisableContextImpl(it))
            unregisterRegistrationsOnDisabled()
        }
    }

    private fun unregisterRegistrationsOnDisabled() {
        // TODO: 在插件关闭时，注销所有插件注册的对象。
    }

    override suspend fun ensureDisabled(operation: Operation) {
        if (isEnabled) {
            disable(operation)
        }
    }

    internal suspend fun ensureDoDisable(operation: Operation) {
        if (isEnabled) {
            doDisable(operation)
        }
    }

    internal suspend fun doUnload(operation: Operation) {
        PluginStateTransition.Unloaded.transit(this, operation) {
            handler.onUnload(PluginUnloadContextImpl(it))
        }
        onDoUnloaded(operation)
    }

    override suspend fun ensureUnloaded(operation: Operation) {
        ensureDisabled(operation)
        if (isLoaded) {
            unload(operation)
        }
    }

    private suspend fun ensureDoUnload(operation: Operation) {
        ensureDoDisable(operation)
        if (isLoaded) {
            doUnload(operation)
        }
    }

    internal suspend fun doRelease(operation: Operation) {
        PluginStateTransition.Released.transit(this, operation) {
            handler.onExit(PluginExitContextImpl(it))
        }
        onDoReleased(operation)
    }

    override suspend fun ensureReleased(operation: Operation) {
        ensureUnloaded(operation)
        if (isAllocated) {
            release(operation)
        }
    }

    private suspend fun ensureDoRelease(operation: Operation) {
        if (isAllocated) {
            doRelease(operation)
        }
        ensureDoUnload(operation)
    }

    abstract suspend fun crash(operation: Operation)

    internal suspend fun doCrash(operation: Operation) {
        PluginStateTransition.Crashed.transit(this, operation) {
            handler.onExit(PluginExitContextImpl(it))
        }
        onDoCrashed(operation)
    }

    internal suspend fun ensureCrashed(operation: Operation) {
        if (!isCrashed) {
            crash(operation)
        }
    }

    override fun toString(): String {
        return "Plugin(signature=$signature, state=$state, operation=$operation)"
    }
}