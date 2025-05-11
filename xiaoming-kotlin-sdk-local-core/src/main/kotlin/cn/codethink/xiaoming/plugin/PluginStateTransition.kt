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

import cn.codethink.xiaoming.event.CancellableEvent
import cn.codethink.xiaoming.event.EventContext
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Operation
import java.util.concurrent.CancellationException
import kotlin.concurrent.write

@InternalApi
sealed class PluginStateTransition<E : CancellableEvent>(
    private val beforeStates: Set<PluginState>,
    private val transitingState: PluginState,
    private val afterState: PluginState
) {
    object Allocated : PluginStateTransition<PluginAllocateEvent>(
        setOf(PluginState.UNALLOCATED), PluginState.ALLOCATING, PluginState.ALLOCATED
    ) {
        override fun createEvent(plugin: AbstractPlugin, operation: Operation): PluginAllocateEvent {
            return PluginAllocateEventImpl(
                plugin = plugin,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    object Loaded : PluginStateTransition<PluginLoadEvent>(
        setOf(PluginState.ALLOCATED), PluginState.LOADING, PluginState.LOADED
    ) {
        override fun createEvent(plugin: AbstractPlugin, operation: Operation): PluginLoadEvent {
            return PluginLoadEventImpl(
                pluginId = plugin.id,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    object Enabled : PluginStateTransition<PluginEnableEvent>(
        setOf(PluginState.LOADED), PluginState.ENABLING, PluginState.ENABLED
    ) {
        override fun createEvent(plugin: AbstractPlugin, operation: Operation): PluginEnableEvent {
            return PluginEnableEventImpl(
                pluginId = plugin.id,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    object Disabled : PluginStateTransition<PluginDisableEvent>(
        setOf(PluginState.ENABLED), PluginState.DISABLING, PluginState.DISABLED
    ) {
        override fun createEvent(plugin: AbstractPlugin, operation: Operation): PluginDisableEvent {
            return PluginDisableEventImpl(
                pluginId = plugin.id,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    object Unloaded : PluginStateTransition<PluginUnloadEvent>(
        setOf(PluginState.LOADED), PluginState.UNLOADING, PluginState.ALLOCATED
    ) {
        override fun createEvent(plugin: AbstractPlugin, operation: Operation): PluginUnloadEvent {
            return PluginUnloadEventImpl(
                pluginId = plugin.id,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    object Released : PluginStateTransition<PluginReleaseEvent>(
        setOf(PluginState.UNALLOCATED, PluginState.CRASHED), PluginState.RELEASING, PluginState.RELEASED
    ) {
        override fun createEvent(plugin: AbstractPlugin, operation: Operation): PluginReleaseEvent {
            return PluginReleaseEventImpl(
                plugin = plugin,
                cause = operation.cause,
                operator = operation.operator,
                time = operation.time,
                id = operation.id
            )
        }
    }

    private fun checkAndSetTransitingState(plugin: AbstractPlugin, operation: Operation): PluginState {
        return with(plugin) {
            lock.write {
                val oldState = stateNoLock
                if (oldState in beforeStates) {
                    stateNoLock = transitingState
                } else {
                    val details = "Details: current operation: ${operation.description}; current state: $stateNoLock; expected state(s): $beforeStates. "
                    when (stateNoLock) {
                        transitingState -> error(
                            "Concurrent state transition to $transitingState (due to: ${plugin.transitingCauseNoLock?.description}) is not allowed. $details"
                        )

                        PluginState.CRASHED -> error("Plugin was crashed due to ${plugin.causeNoLock.description}. $details")
                        afterState -> error("Plugin is already in $afterState state. $details")
                        else -> error("Unexpected plugin state: $stateNoLock. $details")
                    }
                }
                oldState
            }
        }
    }

    private fun checkAndSetAfterStateOrErrorState(plugin: AbstractPlugin): Unit = with(plugin) {
        lock.write {
            stateNoLock = when (stateNoLock) {
                transitingState -> afterState
                PluginState.CRASHED -> PluginState.CRASHED
                else -> error("Unexpected plugin state: $stateNoLock. Plugin is not in $transitingState state while trying to set $afterState state.")
            }
        }
    }

    private fun checkAndSetErrorState(plugin: AbstractPlugin): Unit = with(plugin) {
        lock.write {
            stateNoLock = when (stateNoLock) {
                transitingState,
                PluginState.CRASHED -> PluginState.CRASHED

                else -> error("Unexpected plugin state: $stateNoLock. Plugin is not in $transitingState state while trying to set ${PluginState.CRASHED} state.")
            }
        }
    }

    private fun checkAndSetOldState(plugin: AbstractPlugin, oldState: PluginState): Unit = with(plugin) {
        lock.write {
            stateNoLock = when (stateNoLock) {
                transitingState -> oldState
                else -> error(
                    "Unexpected plugin state: $stateNoLock. " +
                            "Plugin is not in $transitingState state while trying to set to old state $oldState."
                )
            }
        }
    }

    abstract fun createEvent(plugin: AbstractPlugin, operation: Operation): E

    suspend fun <T> transit(plugin: AbstractPlugin, operation: Operation, block: suspend (EventContext<E>) -> T): T {
        val oldState = checkAndSetTransitingState(plugin, operation)

        val event = createEvent(plugin, operation)
        val eventContext = plugin.platform.eventManager.publishEvent(event)

        if (eventContext.event.isCancelled) {
            checkAndSetOldState(plugin, oldState)
            throw CancellationException("Plugin state transiting cancelled")
        }

        try {
            return block(eventContext).also {
                checkAndSetAfterStateOrErrorState(plugin)
            }
        } catch (t: Throwable) {
            checkAndSetErrorState(plugin)
            throw t
        }
    }
}
