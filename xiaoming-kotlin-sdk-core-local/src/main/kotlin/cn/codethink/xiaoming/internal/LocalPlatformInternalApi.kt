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

package cn.codethink.xiaoming.internal

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.EventCause
import cn.codethink.xiaoming.common.PlatformSubject
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.common.currentTimeMillis
import cn.codethink.xiaoming.configuration.LocalPlatformConfiguration
import cn.codethink.xiaoming.connection.ConnectionManagerApi
import cn.codethink.xiaoming.data.LocalPlatformData
import cn.codethink.xiaoming.internal.configuration.LocalPlatformInternalConfiguration
import cn.codethink.xiaoming.internal.event.PlatformStartEvent
import cn.codethink.xiaoming.language.LanguageConfiguration
import cn.codethink.xiaoming.permission.LocalPermissionServiceApi
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Internal API for local platform. Including all the core algorithms and APIs
 * that developers can use.
 *
 * Once developers reached this class, their codes can only run in the local platform.
 *
 * @author Chuanwise
 */
class LocalPlatformInternalApi @JvmOverloads constructor(
    val logger: KLogger,
    val configuration: LocalPlatformInternalConfiguration,
    val subject: PlatformSubject,
    parentJob: Job? = null,
    parentCoroutineContext: CoroutineContext = EmptyCoroutineContext
) : CoroutineScope, AutoCloseable {
    // Coroutine related APIs.
    val supervisorJob = SupervisorJob(parentJob)
    private val coroutineScope = CoroutineScope(parentCoroutineContext + supervisorJob)
    override val coroutineContext: CoroutineContext by coroutineScope::coroutineContext

    /**
     * Lock for changing state.
     */
    val lock = ReentrantReadWriteLock()
    private var stateNoLock = LocalPlatformInternalState.INITIALIZED
    val state: LocalPlatformInternalState
        get() = lock.read { stateNoLock }

    private var platformConfigurationNoLock: LocalPlatformConfiguration? = null
    var platformConfiguration: LocalPlatformConfiguration
        get() = lock.read { platformConfigurationNoLock!! }
        set(value) = lock.write { platformConfigurationNoLock = value }

    private var languageConfigurationNoLock: LanguageConfiguration? = null
    var languageConfiguration: LanguageConfiguration
        get() = lock.read { languageConfigurationNoLock!! }
        set(value) = lock.write { languageConfigurationNoLock = value }

    val data: LocalPlatformData by configuration::data

    val permissionServiceApi = LocalPermissionServiceApi(this)
    val connectionManagerApi = ConnectionManagerApi(this)

    fun start(cause: Cause, subject: Subject) = lock.write {
        assertState(LocalPlatformInternalState.INITIALIZED) {
            "Cannot start the platform when it's not initialized. " +
                    "Notice that DON'T reuse the platform internal API object. To restart it, create a new object please."
        }
        stateNoLock = LocalPlatformInternalState.STARTING
        try {
            var durationTimeMillis = currentTimeMillis
            doStart(cause, subject)
            durationTimeMillis = currentTimeMillis - durationTimeMillis

            logger.debug { "Platform started in ${durationTimeMillis}ms." }
        } catch (exception: Throwable) {
            stateNoLock = LocalPlatformInternalState.STARTING_ERROR
            throw exception
        }
    }

    // Only called when write lock acquired.
    private fun doStart(cause: Cause, subject: Subject) {
        val startingEvent = PlatformStartEvent(cause, subject)
        val startingEventCause = EventCause(startingEvent)
    }

    fun stop(cause: Cause, subject: Subject) {
        TODO()
    }

    override fun close() {
        stop(TextCause("Local platform internal API closed."), subject)
    }
}

fun LocalPlatformInternalApi.assertState(
    state: LocalPlatformInternalState,
    block: () -> String = { "Expected state $state, but got ${this.state}." }
) {
    if (state != this.state) {
        throw IllegalStateException(block())
    }
}
