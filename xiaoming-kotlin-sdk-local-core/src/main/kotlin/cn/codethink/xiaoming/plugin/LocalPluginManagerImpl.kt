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
import cn.codethink.xiaoming.util.PluginDescriptor
import cn.codethink.xiaoming.util.PluginDescriptorImpl
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
            return PluginAllocateEventImpl(
                plugin = plugin,
                allocator = plugin.allocator,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    private val allocatedPluginStateHandler = AllocatedPluginStateChangeHandler()

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
        val allocator: PluginAllocator
    ) : Plugin {
        override val platform: LocalPlatform = this@LocalPluginManagerImpl.platform
        override val descriptor: PluginDescriptor = PluginDescriptorImpl(meta.id)

        var internalStateNoLock: PluginInternalState = PluginInternalState.NOT_YET_ALLOCATED
        var internalState: PluginInternalState
            get() = lock.read { internalStateNoLock }
            set(value) = lock.write { internalStateNoLock = value }

        private var handlerNoLock: PluginHandler? = null

        override val state: PluginState? get() = internalState.state

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

        suspend fun getPluginHandler(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginHandler {
            var pluginHandlerNoLockBeforeLock = handlerNoLock
            if (pluginHandlerNoLockBeforeLock == null) {
                pluginHandlerNoLockBeforeLock = plugin.lock.write {
                    var pluginHandlerNoLockAfterLock = handlerNoLock
                    if (pluginHandlerNoLockAfterLock == null) {
                        pluginHandlerNoLockAfterLock = allocatedPluginStateHandler.changeToAfterState(
                            plugin,
                            operation,
                            policy
                        ) { doPluginAllocation(plugin, it) }
                        handlerNoLock = pluginHandlerNoLockAfterLock
                    }
                    pluginHandlerNoLockAfterLock
                }
            }
            return pluginHandlerNoLockBeforeLock
        }

        suspend fun tryGetPluginHandler(
            plugin: AbstractPlugin,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): PluginHandler? {
            var pluginHandlerNoLockBeforeLock = handlerNoLock
            if (pluginHandlerNoLockBeforeLock == null) {
                pluginHandlerNoLockBeforeLock = plugin.lock.write {
                    var pluginHandlerNoLockAfterLock = handlerNoLock
                    if (pluginHandlerNoLockAfterLock == null) {
                        pluginHandlerNoLockAfterLock = allocatedPluginStateHandler.tryChangeToAfterState(
                            plugin,
                            operation,
                            policy
                        ) { doPluginAllocation(plugin, it) }
                        handlerNoLock = pluginHandlerNoLockAfterLock
                    }
                    pluginHandlerNoLockAfterLock
                }
            }
            return pluginHandlerNoLockBeforeLock
        }

        private suspend fun doLoad(
            event: EventContext<PluginLoadEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ) {
            val pluginHandler = getPluginHandler(this, operation, policy)
            pluginHandler.onLoad(
                context = PluginLoadContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
        }

        private suspend fun tryDoLoad(
            event: EventContext<PluginLoadEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): Boolean {
            val pluginHandler = tryGetPluginHandler(this, operation, policy) ?: return false
            pluginHandler.onLoad(
                context = PluginLoadContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
            return true
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
                tryDoLoad(it, operation, policy)
            } == true
        }

        override suspend fun tryEnsureLoaded(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            return tryLoad(operation, policy)
        }

        private suspend fun doEnable(
            event: EventContext<PluginEnableEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ) {
            val pluginHandler = getPluginHandler(this, operation, policy)
            pluginHandler.onEnable(
                context = PluginEnableContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
        }

        private suspend fun tryDoEnable(
            event: EventContext<PluginEnableEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): Boolean {
            val pluginHandler = tryGetPluginHandler(this, operation, policy) ?: return false
            pluginHandler.onEnable(
                context = PluginEnableContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
            return true
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
                tryDoEnable(it, operation, policy)
            } == true
        }

        override suspend fun tryEnsureEnabled(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            tryEnsureLoaded(operation, policy)
            return enabledPluginStateHandler.tryChangeToAfterState(this, operation, policy) {
                tryDoEnable(it, operation, policy)
            } == true
        }

        private suspend fun doDisable(
            event: EventContext<PluginDisableEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ) {
            val pluginHandler = getPluginHandler(this, operation, policy)
            pluginHandler.onDisable(
                context = PluginDisableContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
        }

        private suspend fun tryDoDisable(
            event: EventContext<PluginDisableEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): Boolean {
            val pluginHandler = tryGetPluginHandler(this, operation, policy) ?: return false
            pluginHandler.onDisable(
                context = PluginDisableContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
            return true
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
                tryDoDisable(it, operation, policy)
            } == true
        }

        override suspend fun tryEnsureDisabled(operation: Operation, policy: PluginStateChangePolicy): Boolean {
            return tryDisable(operation, policy)
        }

        private suspend fun doUnload(
            event: EventContext<PluginUnloadEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ) {
            val pluginHandler = getPluginHandler(this, operation, policy)
            pluginHandler.onUnload(
                context = PluginUnloadContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
        }

        private suspend fun tryDoUnload(
            event: EventContext<PluginUnloadEvent>,
            operation: Operation,
            policy: PluginStateChangePolicy
        ): Boolean {
            val pluginHandler = tryGetPluginHandler(this, operation, policy) ?: return false
            pluginHandler.onUnload(
                context = PluginUnloadContextImpl(
                    platform = platform,
                    event = event,
                    plugin = this,
                )
            )
            return true
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
                tryDoUnload(it, operation, policy)
            } == true
            if (result && tryReleaseUniquePluginLock() != null) {
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

    private inner class LocalRunningPluginImpl(
        meta: PluginMeta,
        mode: PluginMode,
        allocator: PluginAllocator
    ) : AbstractPlugin(meta, mode, allocator), LocalRunningPlugin {
        override val isAllocated: Boolean get() = internalState >= PluginInternalState.ALLOCATED

        val mutableInstances: MutableMap<Platform, AbstractPlugin> = ConcurrentHashMap()
        override val instances: Map<Platform, AbstractPlugin> get() = mutableInstances.toMap()
    }

    private inner class RemoteRunningPluginImpl(
        meta: PluginMeta,
        mode: PluginMode,
        allocator: PluginAllocator
    ) : AbstractPlugin(meta, mode, allocator), RemoteRunningPlugin

    override fun registerPlugin(meta: PluginMeta, mode: PluginMode, allocator: PluginAllocator): Plugin {
        val newPlugin = when (mode) {
            PluginMode.LOCAL -> LocalRunningPluginImpl(meta, mode, allocator)
            PluginMode.REMOTE -> RemoteRunningPluginImpl(meta, mode, allocator)
        }

        val nowPlugin = mutableAvailablePlugins.putIfAbsent(meta.id, meta.version, newPlugin)
        require(nowPlugin === newPlugin) { "Plugin ${meta.id} is already registered" }

        return newPlugin
    }

    override fun getPlugin(namespaceId: NamespaceId): Plugin? {
        return mutablePlugins[namespaceId]
    }
}