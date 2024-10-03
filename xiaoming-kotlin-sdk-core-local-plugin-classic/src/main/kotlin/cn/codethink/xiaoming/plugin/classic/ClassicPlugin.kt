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

package cn.codethink.xiaoming.plugin.classic

import cn.codethink.xiaoming.LocalPlatformApi
import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.plugin.Plugin
import cn.codethink.xiaoming.plugin.PluginDetector
import cn.codethink.xiaoming.plugin.PluginState
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class ClassicPlugin(
    val platformApi: LocalPlatformApi,
    val detector: PluginDetector,
    val configurationDirectoryFile: File,
    val dataDirectoryFile: File,
    val metaConfiguration: ClassicPluginMetaConfiguration,
    override val subject: PluginSubject,
    private val mainCaller: ClassicPluginMainCaller
) : Plugin {
    private val lock = ReentrantReadWriteLock()

    // State related.
    private var stateNoLock: PluginState = PluginState.INITIALIZED
    override val state: PluginState
        get() = lock.read { stateNoLock }

    override fun load(platform: Platform, cause: Cause, subject: Subject) = lock.write {
        stateNoLock = when (stateNoLock) {
            PluginState.INITIALIZED -> PluginState.LOADING
            PluginState.LOADING -> throw IllegalStateException("Concurrent loading plugin.")
            else -> throw IllegalStateException("Cannot load plugin in state $stateNoLock.")
        }

        try {
            mainCaller.onLoad(this, cause, subject)
            stateNoLock = PluginState.LOADED
        } catch (e: Throwable) {
            stateNoLock = PluginState.LOADING_ERROR
            throw e
        }
    }

    override fun enable(platform: Platform, cause: Cause, subject: Subject) = lock.write {
        stateNoLock = when (stateNoLock) {
            PluginState.LOADED -> PluginState.ENABLING
            PluginState.ENABLING -> throw IllegalStateException("Concurrent enabling plugin.")
            else -> throw IllegalStateException("Cannot enable plugin in state $stateNoLock.")
        }

        try {
            mainCaller.onEnable(this, cause, subject)
            stateNoLock = PluginState.ENABLED
        } catch (e: Throwable) {
            stateNoLock = PluginState.ENABLING_ERROR
            throw e
        }
    }

    override fun disable(platform: Platform, cause: Cause, subject: Subject) = lock.write {
        stateNoLock = when (stateNoLock) {
            PluginState.ENABLED -> PluginState.DISABLING
            PluginState.DISABLING -> throw IllegalStateException("Concurrent disabling plugin.")
            else -> throw IllegalStateException("Cannot disable plugin in state $stateNoLock.")
        }

        try {
            mainCaller.onDisable(this, cause, subject)
            stateNoLock = PluginState.DISABLED
        } catch (e: Throwable) {
            stateNoLock = PluginState.DISABLING_ERROR
            throw e
        }
    }

    override fun unload(platform: Platform, cause: Cause, subject: Subject) = lock.write {
        stateNoLock = when (stateNoLock) {
            PluginState.DISABLED -> PluginState.INITIALIZED
            PluginState.INITIALIZED -> throw IllegalStateException("Concurrent unloading plugin.")
            else -> throw IllegalStateException("Cannot unload plugin in state $stateNoLock.")
        }

        try {
            TODO()
        } catch (e: Throwable) {
            stateNoLock = PluginState.LOADING_ERROR
            throw e
        }
    }
}