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

package cn.codethink.xiaoming

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.EventCause
import cn.codethink.xiaoming.common.SdkVersionString
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.doModuleRelatedAction
import cn.codethink.xiaoming.data.LocalPlatformData
import cn.codethink.xiaoming.data.LocalPlatformDataConfiguration
import cn.codethink.xiaoming.event.Event
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.internal.configuration.DefaultLocalPlatformInternalConfiguration
import cn.codethink.xiaoming.internal.event.PlatformStartEvent
import cn.codethink.xiaoming.internal.module.Module
import cn.codethink.xiaoming.internal.module.ModuleContext
import cn.codethink.xiaoming.io.ProtocolLanguageConfiguration
import cn.codethink.xiaoming.io.action.EventSnapshot
import cn.codethink.xiaoming.io.data.DeserializerModule
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.Locale
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface LocalPlatformApi : PlatformApi {
    val modules: List<Module>
    val deserializerModule: DeserializerModule
    val internalApi: LocalPlatformInternalApi
    val dataObjectMapper: ObjectMapper
}

data class DefaultLocalPlatformConfiguration(
    val subject: Subject,
    val language: ProtocolLanguageConfiguration,
    val dataObjectMapper: ObjectMapper,
    val deserializerModule: DeserializerModule,
    val data: LocalPlatformDataConfiguration,
    val parentJob: Job? = null,
    val parentCoroutineContext: CoroutineContext = EmptyCoroutineContext,
    val contributorsDisplayingLimits: Int? = null,
    val additionalContributors: List<String> = emptyList(),
    val locale: Locale = Locale.getDefault(),
    val modules: List<Module> = emptyList(),
    val failOnModuleError: Boolean = true
)

class DefaultLocalPlatformApi(
    private val configuration: DefaultLocalPlatformConfiguration,
    private val logger: KLogger = KotlinLogging.logger { },
) : LocalPlatformApi {
    // Forward to configuration.
    override val subject: Subject by configuration::subject
    override val modules: List<Module> by configuration::modules
    override val deserializerModule: DeserializerModule by configuration::deserializerModule
    override val dataObjectMapper: ObjectMapper by configuration::dataObjectMapper

    // Coroutines related.
    private val supervisorJob = SupervisorJob(configuration.parentJob)
    private val scope: CoroutineScope = CoroutineScope(supervisorJob + configuration.parentCoroutineContext)
    override val coroutineContext: CoroutineContext by scope::coroutineContext

    private val lock = ReentrantReadWriteLock()

    // State related.
    private enum class State {
        INITIALIZED,
        STARTING,
        STARTING_ERROR,
        STARTED,
        STOPPING,
        STOPPING_ERROR,
        STOPPED
    }

    private var stateNoLock: State = State.INITIALIZED
    private val state: State
        get() = lock.read { stateNoLock }

    override lateinit var internalApi: LocalPlatformInternalApi

    fun start(cause: Cause, subject: Subject): Unit = lock.write {
        stateNoLock = when (stateNoLock) {
            State.INITIALIZED -> State.STARTING
            else -> throw IllegalStateException("Cannot start platform when it's in $stateNoLock state.")
        }

        try {
            val platformStartEvent = PlatformStartEvent(cause, subject)
            val platformStartEventCause = EventCause(platformStartEvent)

            logger.info { "Starting default platform ($SdkVersionString)." }
            val context = ModuleContext(this, this.subject, platformStartEventCause)

            // Appreciate to contributors!
            // If there are too many contributors, select randomly and appreciate them.
            val contributorsDisplayingLimits = configuration.contributorsDisplayingLimits
            if (contributorsDisplayingLimits != null && contributorsDisplayingLimits < 0) {
                logger.error { "Configuration item `contributorsDisplayingLimits` MUST be a positive integer!" }
                stateNoLock = State.STARTING_ERROR
                return@write
            }

            val contributors = contributors + configuration.additionalContributors
            logger.info { "We are deeply grateful to all our contributors! " }
            logger.info { "Without your hard work and wisdom, there would be no thriving XiaoMing today! " }

            if (contributorsDisplayingLimits != null && contributors.size > contributorsDisplayingLimits) {
                val randomContributors = contributors.shuffled().take(contributorsDisplayingLimits)
                logger.info {
                            "But we are sorry because configuration item `contributorsDisplayingLimits` set, " +
                                    "we can only randomly select a part of contributors' names to display. "
                }
                logger.info { "Notice that we also have another ${contributors.size - contributorsDisplayingLimits} contributors!" }
                logger.info { "Contributors: ${randomContributors.joinToString(", ")}. (Randomly selected)" }
            } else {
                logger.info { "Contributors: ${contributors.joinToString(", ")}." }
            }

            // 1. Construct internal API.
            internalApi = LocalPlatformInternalApi(
                logger = logger,
                configuration = DefaultLocalPlatformInternalConfiguration(
                    deserializerModule = deserializerModule,
                    dataObjectMapper = configuration.dataObjectMapper,
                    locale = configuration.locale,
                    data = LocalPlatformData(configuration.data.toDataApi(this)),
                    modules = modules,
                    failOnModuleError = configuration.failOnModuleError
                ),
                subject = configuration.subject,
                parentJob = supervisorJob,
                parentCoroutineContext = coroutineContext
            )

            // 2. Start internal API.
            internalApi.start(cause, subject, context)

            // 3. Callback module
            modules.forEach {
                doModuleRelatedAction(
                    logger = logger,
                    description = "callback module '${it.subject.name}' lifecycle methods 'Module#onPlatformStarted'",
                    failOnModuleError = configuration.failOnModuleError
                ) { it.onPlatformStart(context) }
            }

            stateNoLock = State.STARTED
        } catch (e: Throwable) {
            stateNoLock = State.STARTING_ERROR
            throw e
        }
    }

    private fun assertStarted() {
        if (state != State.STARTED) {
            throw IllegalStateException("The platform is not started yet.")
        }
    }

    override fun publishEvent(
        type: String,
        event: Event,
        mutable: Boolean,
        timeout: Long?,
        cause: Cause?
    ): List<EventSnapshot> = lock.read {
        assertStarted()

        emptyList()
    }

    override fun close() = lock.write {
        stateNoLock = when (stateNoLock) {
            State.STARTED -> State.STOPPING
            else -> throw IllegalStateException("Cannot stop platform when it's in $stateNoLock state.")
        }

        try {
            supervisorJob.cancel()

            stateNoLock = State.STOPPED
        } catch (e: Throwable) {
            stateNoLock = State.STOPPING_ERROR
            throw e
        }
    }
}