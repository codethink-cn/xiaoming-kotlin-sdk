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

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.LocalPlatformConfiguration
import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.event.CancellableEvent
import cn.codethink.xiaoming.event.EventContext
import cn.codethink.xiaoming.util.DualKeyMap
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MutableDualKeyMapImpl
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.PluginSubjectDescriptor
import cn.codethink.xiaoming.util.PluginSubjectDescriptorImpl
import cn.codethink.xiaoming.util.Version
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.jvm.jvmName

@OptIn(InternalApi::class)
class LocalPluginManagerImpl(
    override val platform: LocalPlatform,
    configuration: LocalPlatformConfiguration
) : LocalPluginManager {
    override val platformClassLoader: ClassLoader = configuration.platformClassLoader
    override val environmentClassLoader: ClassLoader = configuration.environmentClassLoader

    private val logger: KLogger = KotlinLogging.logger(PluginManager::class.jvmName)

    // 具有相同 ID，但用不同版本的插件可以并存，但是只有一个可以加载。
    private val mutableAvailablePlugins = MutableDualKeyMapImpl<NamespaceId, Version, AbstractPlugin>()
    override val availablePlugins: DualKeyMap<NamespaceId, Version, Plugin> get() = mutableAvailablePlugins

    // 插件一旦加载，便需先将自己置于其中。以免相同 ID，不同版本的插件被同时加载。
    private var mutablePlugins = ConcurrentHashMap<NamespaceId, Plugin>()
    override val plugins: Map<NamespaceId, Plugin> get() = mutablePlugins.toMap()

    private enum class PluginInternalState(val state: PluginState? = null) {
        NOT_YET_ALLOCATED,

        ALLOCATING,
        ALLOCATING_ERRORED,

        ALLOCATED(PluginState.ALLOCATED),

        LOADING(PluginState.LOADING),
        LOADING_ERRORED(PluginState.LOADING_ERRORED),
        LOADED(PluginState.LOADED),

        ENABLING(PluginState.ENABLING),
        ENABLING_ERRORED(PluginState.ENABLING_ERRORED),
        ENABLED(PluginState.ENABLED),

        DISABLING(PluginState.DISABLING),
        DISABLING_ERRORED(PluginState.DISABLING_ERRORED),
        DISABLED(PluginState.DISABLED),

        UNLOADING(PluginState.UNLOADING),
        UNLOADING_ERRORED(PluginState.UNLOADING_ERRORED),
    }

    // 插件稳定状态：NotYetAllocated、Allocated、Loaded、Enabled 中的某个状态。
    private abstract inner class PluginStateChangeHandler<E : CancellableEvent>(
        val beforeStates: Set<PluginInternalState>,
        val changingState: PluginInternalState,
        val changingErrorState: PluginInternalState,
        val afterState: PluginInternalState
    ) {
        fun checkAndSetChangingState(plugin: AbstractPlugin, policy: PluginStateChangePolicy): PluginInternalState {
            return with(plugin) {
                lock.write {
                    val oldState = internalStateNoLock
                    if (oldState in beforeStates) {
                        internalStateNoLock = changingState
                    } else {
                        internalStateNoLock = when (internalStateNoLock) {
                            changingState -> error("Concurrent state transition to $changingState")
                            changingErrorState -> {
                                if (policy.ignorePreviousError) {
                                    logger.warn {
                                        "Plugin ${plugin.meta.id} is in $changingErrorState state. " +
                                                "Retrying to change state caused by policy `ignorePreviousError` set to true. "
                                    }
                                    changingState
                                } else {
                                    throw IllegalArgumentException(
                                        "Last plugin state transition failed, resulting in $changingErrorState state. " +
                                                "Try to set `force` to true and try again. " +
                                                "Note that `force` will ignore the error and may cause unexpected behavior."
                                    )
                                }
                            }

                            afterState -> error("Plugin is already in $afterState state")
                            else -> error("Unexpected plugin state: $internalStateNoLock. Note that only $beforeStates and $changingErrorState states can be changed to $changingState.")
                        }
                    }
                    oldState
                }
            }
        }

        fun tryCheckAndSetTransferState(plugin: AbstractPlugin, policy: PluginStateChangePolicy): PluginInternalState? {
            return with(plugin) {
                lock.write {
                    val oldState = internalStateNoLock
                    if (oldState in beforeStates) {
                        internalStateNoLock = changingState
                    } else {
                        internalStateNoLock = when (internalStateNoLock) {
                            changingState -> return null
                            changingErrorState -> {
                                if (policy.ignorePreviousError) {
                                    logger.warn {
                                        "Plugin ${plugin.meta.id} is in $changingErrorState state. " +
                                                "Retrying to change state caused by policy `ignorePreviousError` set to true. "
                                    }
                                    changingState
                                } else {
                                    return null
                                }
                            }

                            afterState -> return null
                            else -> return null
                        }
                    }
                    oldState
                }
            }
        }

        fun checkAndSetAfterState(plugin: AbstractPlugin): Unit = with(plugin) {
            lock.write {
                internalStateNoLock = when (internalStateNoLock) {
                    changingState -> afterState
                    changingErrorState -> changingErrorState

                    else -> error("Unexpected plugin state: $internalStateNoLock. Plugin is not in $changingState state while trying to set $afterState state.")
                }
            }
        }

        fun checkAndSetErrorState(plugin: AbstractPlugin): Unit = with(plugin) {
            plugin.lock.write {
                plugin.internalStateNoLock = when (plugin.internalStateNoLock) {
                    changingState,
                    changingErrorState -> changingErrorState

                    else -> error("Unexpected plugin state: $internalStateNoLock. Plugin is not in $changingState state while trying to set $changingErrorState state.")
                }
            }
        }

        fun checkAndSetOldState(plugin: AbstractPlugin, oldState: PluginInternalState): Unit = with(plugin) {
            lock.write {
                internalStateNoLock = when (internalStateNoLock) {
                    changingState -> oldState
                    else -> error(
                        "Unexpected plugin state: $internalStateNoLock. " +
                                "Plugin is not in $changingState state while trying to set to old state $oldState."
                    )
                }
            }
        }

        abstract fun createEvent(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): E

        suspend inline fun <T> changeToAfterState(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy,
            block: (EventContext<E>) -> T
        ): T {
            val oldState = checkAndSetChangingState(plugin, policy)

            val event = createEvent(plugin, operation, policy)
            val eventContext = plugin.platform.eventManager.publishCancellableEvent(event)

            if (eventContext.isCancelled) {
                checkAndSetOldState(plugin, oldState)
                throw CancellationException("Plugin state changing cancelled")
            }

            try {
                return block(eventContext).also {
                    checkAndSetAfterState(plugin)
                }
            } catch (t: Throwable) {
                if (policy.ignoreCurrentError) {
                    logger.warn(t) {
                        "Plugin state changing failed, but policy `ignoreCurrentError` is true." +
                                " The exception will be ignored, and the plugin will be set to $afterState state."
                    }
                    checkAndSetAfterState(plugin)
                } else {
                    checkAndSetErrorState(plugin)
                }
                throw t
            }
        }

        suspend inline fun <T> tryChangeToAfterState(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy,
            block: (EventContext<E>) -> T
        ): T? {
            val oldState = tryCheckAndSetTransferState(plugin, policy) ?: return null

            val event = createEvent(plugin, operation, policy)
            val eventContext = plugin.platform.eventManager.publishCancellableEvent(event)

            if (eventContext.isCancelled) {
                checkAndSetOldState(plugin, oldState)
                return null
            }

            try {
                return block(eventContext).also {
                    checkAndSetAfterState(plugin)
                }
            } catch (t: Throwable) {
                if (policy.ignoreCurrentError) {
                    logger.warn(t) {
                        "Plugin state changing failed, but policy `ignoreCurrentError` is true." +
                                " The exception will be ignored, and the plugin will be set to $afterState state."
                    }
                    checkAndSetAfterState(plugin)
                    return null
                } else {
                    checkAndSetErrorState(plugin)
                }
                throw t
            }
        }
    }

    private inner class AllocatedPluginStateChangeHandler : PluginStateChangeHandler<PluginAllocateEvent>(
        beforeStates = setOf(PluginInternalState.NOT_YET_ALLOCATED),
        changingState = PluginInternalState.ALLOCATING,
        changingErrorState = PluginInternalState.ALLOCATING_ERRORED,
        afterState = PluginInternalState.ALLOCATED
    ) {
        override fun createEvent(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginAllocateEvent {
            val handlerHolder = plugin.handlerHolder as AllocatorPluginHandlerHolder
            return PluginAllocateEventImpl(
                plugin = plugin,
                allocator = handlerHolder.allocator,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    private val allocatedPluginStateHandler = AllocatedPluginStateChangeHandler()

    // 插件的起始状态可能是 ALLOCATED 或 NOT_YET_ALLOCATED，
    // PluginHandlerHolder 是统一获取插件主类操作，视情况分配插件主类的工具。
    private interface PluginHandlerHolder {
        val initialInternalState: PluginInternalState

        suspend fun getPluginHandler(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginHandler

        suspend fun tryGetPluginHandler(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginHandler?
    }

    private inner class AllocatorPluginHandlerHolder(val allocator: PluginAllocator) : PluginHandlerHolder {
        override val initialInternalState: PluginInternalState = PluginInternalState.NOT_YET_ALLOCATED
        private var pluginHandlerNoLock: PluginHandler? = null

        private fun doPluginAllocation(plugin: Plugin, event: EventContext<PluginAllocateEvent>): PluginHandler {
            return allocator.allocatePluginHandler(
                context = PluginAllocateContextImpl(
                    platform = platform,
                    event = event,
                    allocator = allocator,
                    meta = plugin.meta,
                    plugin = plugin
                )
            )
        }

        override suspend fun getPluginHandler(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginHandler {
            var pluginHandlerNoLockBeforeLock = pluginHandlerNoLock
            if (pluginHandlerNoLockBeforeLock == null) {
                pluginHandlerNoLockBeforeLock = plugin.lock.write {
                    var pluginHandlerNoLockAfterLock = pluginHandlerNoLock
                    if (pluginHandlerNoLockAfterLock == null) {
                        pluginHandlerNoLockAfterLock = allocatedPluginStateHandler.changeToAfterState(
                            plugin,
                            operation,
                            policy
                        ) { doPluginAllocation(plugin, it) }
                        pluginHandlerNoLock = pluginHandlerNoLockAfterLock
                    }
                    pluginHandlerNoLockAfterLock
                }
            }
            return pluginHandlerNoLockBeforeLock
        }

        override suspend fun tryGetPluginHandler(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginHandler? {
            var pluginHandlerNoLockBeforeLock = pluginHandlerNoLock
            if (pluginHandlerNoLockBeforeLock == null) {
                pluginHandlerNoLockBeforeLock = plugin.lock.write {
                    var pluginHandlerNoLockAfterLock = pluginHandlerNoLock
                    if (pluginHandlerNoLockAfterLock == null) {
                        pluginHandlerNoLockAfterLock = allocatedPluginStateHandler.tryChangeToAfterState(
                            plugin,
                            operation,
                            policy
                        ) { doPluginAllocation(plugin, it) }
                        pluginHandlerNoLock = pluginHandlerNoLockAfterLock
                    }
                    pluginHandlerNoLockAfterLock
                }
            }
            return pluginHandlerNoLockBeforeLock
        }
    }

    private class AllocatedPluginHandlerHolder(val handler: PluginHandler) : PluginHandlerHolder {
        override val initialInternalState: PluginInternalState = PluginInternalState.ALLOCATED

        override suspend fun getPluginHandler(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginHandler = handler

        override suspend fun tryGetPluginHandler(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginHandler = handler
    }

    private inner class LoadedPluginStateChangeHandler : PluginStateChangeHandler<PluginLoadEvent>(
        beforeStates = setOf(PluginInternalState.ALLOCATED),
        changingState = PluginInternalState.LOADING,
        changingErrorState = PluginInternalState.LOADING_ERRORED,
        afterState = PluginInternalState.LOADED
    ) {
        override fun createEvent(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginLoadEvent {
            return PluginLoadEventImpl(
                pluginId = plugin.id,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    private val loadedPluginStateHandler = LoadedPluginStateChangeHandler()

    private inner class EnabledPluginStateChangeHandler : PluginStateChangeHandler<PluginEnableEvent>(
        beforeStates = setOf(PluginInternalState.LOADED),
        changingState = PluginInternalState.ENABLING,
        changingErrorState = PluginInternalState.ENABLING_ERRORED,
        afterState = PluginInternalState.ENABLED
    ) {
        override fun createEvent(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginEnableEvent {
            return PluginEnableEventImpl(
                pluginId = plugin.id,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    private val enabledPluginStateHandler = EnabledPluginStateChangeHandler()

    private inner class DisabledPluginStateChangeHandler : PluginStateChangeHandler<PluginDisableEvent>(
        beforeStates = setOf(PluginInternalState.ENABLED),
        changingState = PluginInternalState.DISABLING,
        changingErrorState = PluginInternalState.DISABLING_ERRORED,
        afterState = PluginInternalState.DISABLED
    ) {
        override fun createEvent(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginDisableEvent {
            return PluginDisableEventImpl(
                pluginId = plugin.id,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    private val disabledPluginStateHandler = DisabledPluginStateChangeHandler()

    private inner class UnloadedPluginStateChangeHandler : PluginStateChangeHandler<PluginUnloadEvent>(
        beforeStates = setOf(PluginInternalState.LOADED),
        changingState = PluginInternalState.UNLOADING,
        changingErrorState = PluginInternalState.UNLOADING_ERRORED,
        afterState = PluginInternalState.ALLOCATED
    ) {
        override fun createEvent(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginUnloadEvent {
            return PluginUnloadEventImpl(
                pluginId = plugin.id,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    private val unloadedPluginStateHandler = UnloadedPluginStateChangeHandler()

    private abstract inner class AbstractPlugin(
        final override val meta: PluginMeta,
        final override val mode: PluginMode,

        val handlerHolder: PluginHandlerHolder
    ) : Plugin {
        override val platform: LocalPlatform = this@LocalPluginManagerImpl.platform
        override val descriptor: PluginSubjectDescriptor = PluginSubjectDescriptorImpl(meta.id)

        var internalStateNoLock: PluginInternalState = handlerHolder.initialInternalState
        var internalState: PluginInternalState
            get() = lock.read { internalStateNoLock }
            set(value) = lock.write { internalStateNoLock = value }

        override val state: PluginState get() = internalState.state ?: error("Plugin is not allocated yet")

        override val isAllocated: Boolean get() = internalState >= PluginInternalState.ALLOCATED
        override val isLoaded: Boolean get() = lock.read { internalStateNoLock >= PluginInternalState.LOADED && internalStateNoLock < PluginInternalState.UNLOADING }
        override val isEnabled: Boolean get() = internalState == PluginInternalState.ENABLED

        override val provisions: MutableMap<NamespaceId, Version> = ConcurrentHashMap()
        val lock = ReentrantReadWriteLock()

        // 尝试为本插件获取 LOAD 锁。返回持有独占当前 ID 启动权的插件。若为 this 表示获取成功。
        private fun tryAcquireUniquePluginLock(): Plugin {
            return mutablePlugins.computeIfAbsent(id) { this }
        }

        private fun acquireUniquePluginLock() {
            val nowPlugin = tryAcquireUniquePluginLock()
            check(nowPlugin == this) {
                "Plugin ${meta.id} is already loaded and its version is ${nowPlugin.version}. Fail to acquire unique plugin lock for another version $version."
            }
        }

        // 插件被成功 UNLOAD，释放独占当前 ID 的锁。若返回 null 表示成功（也可能从未获取），否则失败。
        private fun tryReleaseUniquePluginLock(): Plugin? {
            return mutablePlugins.computeIfPresent(id) { _, nowPlugin ->
                if (nowPlugin === this) {
                    null
                } else {
                    nowPlugin
                }
            }
        }

        private fun releaseUniquePluginLock() {
            val nowPlugin = tryReleaseUniquePluginLock()
            check(nowPlugin == null) {
                "Plugin ${meta.id} is not loaded and its version is ${nowPlugin?.version}. Fail to release unique plugin lock for another version $version."
            }
        }

        private suspend fun doLoad(
            event: EventContext<PluginLoadEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ) {
            val pluginHandler = handlerHolder.getPluginHandler(this, operation, policy)
            pluginHandler.onLoad(
                context = PluginLoadContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
        }

        override suspend fun load(operation: Operation, policy: PluginStateChangePolicy) {
            acquireUniquePluginLock()
            loadedPluginStateHandler.changeToAfterState(this, operation, policy) { doLoad(it, operation, policy) }
        }

        override suspend fun ensureLoaded(operation: Operation, policy: PluginStateChangePolicy) {
            tryLoad(operation, policy)
        }

        override suspend fun tryLoad(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            if (tryAcquireUniquePluginLock() !== this) {
                return false
            }
            return loadedPluginStateHandler.tryChangeToAfterState(this, operation, policy) {
                doLoad(it, operation, policy)
            } != null
        }

        override suspend fun tryEnsureLoaded(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            return tryLoad(operation, policy)
        }

        private suspend fun doEnable(
            event: EventContext<PluginEnableEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ) {
            val pluginHandler = handlerHolder.getPluginHandler(this, operation, policy)
            pluginHandler.onEnable(
                context = PluginEnableContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
        }

        override suspend fun enable(operation: Operation, policy: PluginStateChangePolicy) {
            enabledPluginStateHandler.changeToAfterState(this, operation, policy) { doEnable(it, operation, policy) }
        }

        override suspend fun ensureEnabled(operation: Operation, policy: PluginStateChangePolicy) {
            ensureLoaded(operation, policy)
            tryEnable(operation, policy)
        }

        override suspend fun tryEnable(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            return enabledPluginStateHandler.tryChangeToAfterState(this, operation, policy) {
                doEnable(it, operation, policy)
            } != null
        }

        override suspend fun tryEnsureEnabled(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            tryEnsureLoaded(operation, policy)
            return enabledPluginStateHandler.tryChangeToAfterState(this, operation, policy) {
                doEnable(it, operation, policy)
            } != null
        }

        private suspend fun doDisable(
            event: EventContext<PluginDisableEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ) {
            val pluginHandler = handlerHolder.getPluginHandler(this, operation, policy)
            pluginHandler.onDisable(
                context = PluginDisableContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
        }

        override suspend fun disable(operation: Operation, policy: PluginStateChangePolicy) {
            disabledPluginStateHandler.changeToAfterState(this, operation, policy) { doDisable(it, operation, policy) }
        }

        override suspend fun ensureDisabled(operation: Operation, policy: PluginStateChangePolicy) {
            if (isEnabled) {
                disable(operation, policy)
            }
        }

        override suspend fun tryDisable(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            return disabledPluginStateHandler.tryChangeToAfterState(this, operation, policy) {
                doDisable(it, operation, policy)
            } != null
        }

        override suspend fun tryEnsureDisabled(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            return tryDisable(operation, policy)
        }

        private suspend fun doUnload(
            event: EventContext<PluginUnloadEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ) {
            val pluginHandler = handlerHolder.getPluginHandler(this, operation, policy)
            pluginHandler.onUnload(
                context = PluginUnloadContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
        }

        override suspend fun unload(operation: Operation, policy: PluginStateChangePolicy) {
            unloadedPluginStateHandler.changeToAfterState(this, operation, policy) { doUnload(it, operation, policy) }
            releaseUniquePluginLock()
        }

        override suspend fun ensureUnloaded(operation: Operation, policy: PluginStateChangePolicy) {
            if (isEnabled) {
                disable(operation, policy)
            }
            if (isLoaded) {
                unload(operation, policy)
            }
        }

        override suspend fun tryUnload(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            val result = unloadedPluginStateHandler.tryChangeToAfterState(this, operation, policy) {
                doUnload(it, operation, policy)
            } != null
            if (tryReleaseUniquePluginLock() != null) {
                return false
            }
            return result
        }

        override suspend fun tryEnsureUnloaded(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            if (isEnabled) {
                tryDisable(operation, policy)
            }
            return tryUnload(operation, policy)
        }
    }

    private inner class LocalPluginImpl(
        meta: PluginMeta,
        mode: PluginMode,
        handlerHolder: PluginHandlerHolder
    ) : AbstractPlugin(meta, mode, handlerHolder), LocalPlugin {
        val mutableEntries: MutableMap<Platform, PluginEntry> = ConcurrentHashMap()
        override val entries: Map<Platform, PluginEntry> get() = mutableEntries.toMap()
    }

    private inner class RemotePluginImpl(
        meta: PluginMeta,
        mode: PluginMode,
        handlerHolder: PluginHandlerHolder
    ) : AbstractPlugin(meta, mode, handlerHolder), RemotePlugin

    override fun registerPlugin(meta: PluginMeta, mode: PluginMode, allocator: PluginAllocator): Plugin {
        val pluginHandlerHolder = AllocatorPluginHandlerHolder(allocator)
        return doRegisterPlugin(meta, mode, pluginHandlerHolder)
    }

    override fun registerPlugin(meta: PluginMeta, mode: PluginMode, handler: PluginHandler): Plugin {
        val pluginHandlerHolder = AllocatedPluginHandlerHolder(handler)
        return doRegisterPlugin(meta, mode, pluginHandlerHolder)
    }

    private fun doRegisterPlugin(meta: PluginMeta, mode: PluginMode, handlerHolder: PluginHandlerHolder): Plugin {
        val newPlugin = when (mode) {
            PluginMode.LOCAL -> LocalPluginImpl(meta, mode, handlerHolder)
            PluginMode.REMOTE -> RemotePluginImpl(meta, mode, handlerHolder)
        }

        val nowPlugin = mutableAvailablePlugins.putIfAbsent(meta.id, meta.version, newPlugin)
        require(nowPlugin === newPlugin) { "Plugin ${meta.id} is already registered" }

        return newPlugin
    }

    override fun getPlugin(namespaceId: NamespaceId): Plugin? {
        return availablePlugins.toMapByKey1(namespaceId).values.singleOrNull { it.isLoaded }
    }
}