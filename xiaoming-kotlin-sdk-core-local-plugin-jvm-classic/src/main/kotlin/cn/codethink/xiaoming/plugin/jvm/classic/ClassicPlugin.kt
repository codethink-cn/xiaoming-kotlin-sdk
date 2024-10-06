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

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.LocalPlatformApi
import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.PluginSubjectDescriptor
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.currentThreadCauseOrFail
import cn.codethink.xiaoming.plugin.PluginDetector
import cn.codethink.xiaoming.plugin.PluginLevel
import cn.codethink.xiaoming.plugin.PluginMode
import cn.codethink.xiaoming.plugin.PluginRuntimeMeta
import cn.codethink.xiaoming.plugin.PluginTask
import cn.codethink.xiaoming.plugin.jvm.LocalJvmPlugin
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class ClassicPlugin(
    val platformApi: LocalPlatformApi,
    detector: PluginDetector,
    val distributionFile: File,
    val distributionDirectoryFile: File,
    val configurationDirectoryFile: File,
    val dataDirectoryFile: File,
    level: PluginLevel,
    override val meta: ClassicPluginMeta,
    override val descriptor: PluginSubjectDescriptor,
    private val invoker: PluginMainInvoker
) : LocalJvmPlugin(detector) {
    private val lock = ReentrantReadWriteLock()

    class ClassicPluginRuntimeMeta(
        override val level: PluginLevel,

        @JsonIgnore
        private val plugin: ClassicPlugin
    ) : PluginRuntimeMeta {
        override val state by plugin::state
        override val mode: PluginMode = PluginMode.LOCAL

        override val isLoaded: Boolean
            get() = state.loaded
        override val isEnabled: Boolean
            get() = state.enabled
        override val isErrored: Boolean
            get() = state.errored
    }

    override val runtime = ClassicPluginRuntimeMeta(level, this)

    private val mutableTasks: MutableMap<SegmentId, PluginTask> = ConcurrentHashMap()
    override val tasks: Map<SegmentId, PluginTask>
        get() = mutableTasks.toMap()

    // State related.
    private var stateNoLock: ClassicalPluginState = ClassicalPluginState.ALLOCATED
    private val state: ClassicalPluginState
        get() = lock.read { stateNoLock }

    override fun load(platform: Platform, cause: Cause?): Unit = lock.write {
        val finalCause = cause ?: currentThreadCauseOrFail()

        stateNoLock = when (stateNoLock) {
            ClassicalPluginState.ALLOCATED -> ClassicalPluginState.LOADING
            ClassicalPluginState.LOADING -> throw IllegalStateException("Concurrent loading plugin.")
            else -> throw IllegalStateException("Cannot load plugin in state $stateNoLock.")
        }

        try {
            invoker.invokeOnLoad(this, finalCause)
            stateNoLock = ClassicalPluginState.LOADED
        } catch (e: Throwable) {
            stateNoLock = ClassicalPluginState.LOADING_ERROR
            throw e
        }
    }

    override fun enable(platform: Platform, cause: Cause?): Unit = lock.write {
        val finalCause = cause ?: currentThreadCauseOrFail()

        stateNoLock = when (stateNoLock) {
            ClassicalPluginState.LOADED -> ClassicalPluginState.ENABLING
            ClassicalPluginState.ENABLING -> throw IllegalStateException("Concurrent enabling plugin.")
            else -> throw IllegalStateException("Cannot enable plugin in state $stateNoLock.")
        }

        try {
            invoker.invokeOnEnable(this, finalCause)
            stateNoLock = ClassicalPluginState.ENABLED
        } catch (e: Throwable) {
            stateNoLock = ClassicalPluginState.ENABLING_ERROR
            throw e
        }
    }

    override fun disable(platform: Platform, cause: Cause?): Unit = lock.write {
        val finalCause = cause ?: currentThreadCauseOrFail()

        stateNoLock = when (stateNoLock) {
            ClassicalPluginState.ENABLED -> ClassicalPluginState.DISABLING
            ClassicalPluginState.DISABLING -> throw IllegalStateException("Concurrent disabling plugin.")
            else -> throw IllegalStateException("Cannot disable plugin in state $stateNoLock.")
        }

        try {
            invoker.invokeOnDisable(this, finalCause)
            stateNoLock = ClassicalPluginState.DISABLED
        } catch (e: Throwable) {
            stateNoLock = ClassicalPluginState.DISABLING_ERROR
            throw e
        }
    }

    override fun unload(platform: Platform, cause: Cause?): Unit = lock.write {
        val finalCause = cause ?: currentThreadCauseOrFail()

        stateNoLock = when (stateNoLock) {
            ClassicalPluginState.DISABLED -> ClassicalPluginState.ALLOCATED
            ClassicalPluginState.ALLOCATED -> throw IllegalStateException("Concurrent unloading plugin.")
            else -> throw IllegalStateException("Cannot unload plugin in state $stateNoLock.")
        }

        try {
            TODO()
        } catch (e: Throwable) {
            stateNoLock = ClassicalPluginState.LOADING_ERROR
            throw e
        }
    }
}