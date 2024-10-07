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

package cn.codethink.xiaoming.internal

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.common.AutoClosableSubject
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.common.doModuleRelatedAction
import cn.codethink.xiaoming.configuration.LocalPlatformConfiguration
import cn.codethink.xiaoming.connection.ConnectionManagerApi
import cn.codethink.xiaoming.data.LocalPlatformData
import cn.codethink.xiaoming.internal.configuration.LocalPlatformInternalConfiguration
import cn.codethink.xiaoming.internal.module.ModuleContext
import cn.codethink.xiaoming.language.LanguageConfiguration
import cn.codethink.xiaoming.permission.LocalPermissionServiceApi
import cn.codethink.xiaoming.plugin.LocalPluginManagerApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

/**
 * Internal API for local platform. Including all the core algorithms and APIs
 * that developers can use.
 *
 * Once developers reached this class, their codes can only run in the local platform.
 *
 * @author Chuanwise
 */
class LocalPlatformInternalApi(
    val internalConfiguration: LocalPlatformInternalConfiguration,
) : CoroutineScope, AutoClosableSubject {
    val logger by internalConfiguration::logger
    override val descriptor by internalConfiguration::descriptor

    // Coroutine related APIs.
    val supervisorJob = SupervisorJob(internalConfiguration.parentJob)
    private val coroutineScope = CoroutineScope(internalConfiguration.parentCoroutineContext + supervisorJob)
    override val coroutineContext: CoroutineContext by coroutineScope::coroutineContext

    /**
     * Lock for changing state.
     */
    val lock = ReentrantReadWriteLock()

    /**
     * The platform that this internal API belongs to.
     * Initialized in [start].
     */
    private var mutPlatform: LocalPlatform? = null
    val platform: LocalPlatform
        get() = lock.read { mutPlatform } ?: throw IllegalStateException("Platform not started yet.")

    /**
     * The state of the platform internal API.
     */
    private enum class State {
        ALLOCATED,
        STARTING,
        STARTING_ERROR,
        STARTED,
        STOPPING,
        STOPPING_ERROR,
        STOPPED
    }

    private var stateNoLock = State.ALLOCATED
    private val state: State
        get() = lock.read { stateNoLock }

    /**
     * The configuration of the platform.
     */
    private var platformConfigurationNoLock: LocalPlatformConfiguration? = null
    var platformConfiguration: LocalPlatformConfiguration
        get() = lock.read { platformConfigurationNoLock!! }
        set(value) = lock.write { platformConfigurationNoLock = value }

    /**
     * The language of the platform.
     */
    private var languageConfigurationNoLock: LanguageConfiguration? = null
    var languageConfiguration: LanguageConfiguration
        get() = lock.read { languageConfigurationNoLock!! }
        set(value) = lock.write { languageConfigurationNoLock = value }

    /**
     * The data of the platform.
     */
    val data: LocalPlatformData by internalConfiguration::data

    /**
     * The subsystems of the platform.
     */
    val pluginManagerApi = LocalPluginManagerApi(this)
    val permissionServiceApi = LocalPermissionServiceApi(this)
    val connectionManagerApi = ConnectionManagerApi(this)

    fun start(platform: LocalPlatform, cause: Cause, context: ModuleContext) = lock.write {
        stateNoLock = when (stateNoLock) {
            State.ALLOCATED -> State.STARTING
            else -> throw IllegalStateException("Cannot start platform when it's not initialized.")
        }

        try {
            logger.info { "Starting platform internal API." }
            mutPlatform = platform

            // Call module lifecycle methods.
            internalConfiguration.modules.forEach {
                doModuleRelatedAction(
                    logger = logger,
                    description = "callback module '${it.subject.name}' lifecycle methods 'Module#onPlatformStarting'",
                    failOnModuleError = internalConfiguration.failOnModuleError
                ) { it.onPlatformStarting(context) }
            }

            stateNoLock = State.STARTED
        } catch (e: Exception) {
            stateNoLock = State.STARTING_ERROR
            throw e
        }
    }

    fun stop(cause: Cause, subject: SubjectDescriptor) {
        TODO()
    }

    override fun close(cause: Cause) {
        TODO("Not yet implemented")
    }

    override fun close() {
        close(TextCause("Local platform internal API closed.", descriptor))
    }
}