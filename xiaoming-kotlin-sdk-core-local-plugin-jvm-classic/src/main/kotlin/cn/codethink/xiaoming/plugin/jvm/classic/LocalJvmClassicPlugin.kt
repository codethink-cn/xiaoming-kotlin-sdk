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

class LocalJvmClassicPlugin(
    val platformApi: LocalPlatformApi,
    val distributionFile: File,
    val distributionDirectoryFile: File,
    val configurationDirectoryFile: File,
    val dataDirectoryFile: File,
    level: PluginLevel,
    override val meta: LocalJvmClassicPluginMeta,
    private val entry: LocalJvmClassicPluginMainEntry
) : LocalJvmPlugin() {
    private val lock = ReentrantReadWriteLock()
    override val descriptor: PluginSubjectDescriptor = PluginSubjectDescriptor(meta.id)

    class ClassicPluginRuntimeMeta(
        override val level: PluginLevel,

        @JsonIgnore
        private val plugin: LocalJvmClassicPlugin
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
    private var stateNoLock: LocalJvmClassicPluginState = LocalJvmClassicPluginState.ALLOCATED
    private val state: LocalJvmClassicPluginState
        get() = lock.read { stateNoLock }

    override fun load(platform: Platform, cause: Cause): Unit = lock.write {
        stateNoLock = when (stateNoLock) {
            LocalJvmClassicPluginState.ALLOCATED -> LocalJvmClassicPluginState.LOADING
            LocalJvmClassicPluginState.LOADING -> throw IllegalStateException("Concurrent loading plugin.")
            else -> throw IllegalStateException("Cannot load plugin in state $stateNoLock.")
        }

        try {
            entry.onLoad(this, cause)
            stateNoLock = LocalJvmClassicPluginState.LOADED
        } catch (e: Throwable) {
            stateNoLock = LocalJvmClassicPluginState.LOADING_ERROR
            throw e
        }
    }

    override fun enable(platform: Platform, cause: Cause): Unit = lock.write {
        stateNoLock = when (stateNoLock) {
            LocalJvmClassicPluginState.LOADED -> LocalJvmClassicPluginState.ENABLING
            LocalJvmClassicPluginState.ENABLING -> throw IllegalStateException("Concurrent enabling plugin.")
            else -> throw IllegalStateException("Cannot enable plugin in state $stateNoLock.")
        }

        try {
            entry.onEnable(this, cause)
            stateNoLock = LocalJvmClassicPluginState.ENABLED
        } catch (e: Throwable) {
            stateNoLock = LocalJvmClassicPluginState.ENABLING_ERROR
            throw e
        }
    }

    override fun disable(platform: Platform, cause: Cause): Unit = lock.write {
        stateNoLock = when (stateNoLock) {
            LocalJvmClassicPluginState.ENABLED -> LocalJvmClassicPluginState.DISABLING
            LocalJvmClassicPluginState.DISABLING -> throw IllegalStateException("Concurrent disabling plugin.")
            else -> throw IllegalStateException("Cannot disable plugin in state $stateNoLock.")
        }

        try {
            entry.onDisable(this, cause)
            stateNoLock = LocalJvmClassicPluginState.DISABLED
        } catch (e: Throwable) {
            stateNoLock = LocalJvmClassicPluginState.DISABLING_ERROR
            throw e
        }
    }

    override fun unload(platform: Platform, cause: Cause): Unit = lock.write {
        stateNoLock = when (stateNoLock) {
            LocalJvmClassicPluginState.DISABLED -> LocalJvmClassicPluginState.ALLOCATED
            LocalJvmClassicPluginState.ALLOCATED -> throw IllegalStateException("Concurrent unloading plugin.")
            else -> throw IllegalStateException("Cannot unload plugin in state $stateNoLock.")
        }

        try {
            TODO()
        } catch (e: Throwable) {
            stateNoLock = LocalJvmClassicPluginState.LOADING_ERROR
            throw e
        }
    }

    override fun toString(): String {
        return "LocalJvmClassicPlugin(meta=$meta)"
    }
}