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

cimport cn.codethink.xiaoming.common.DEFAULT_LOCALE_LANGUAGE_FILE_PATH
import cn.codethink.xiaoming.common.EventCause
import cn.codethink.xiaoming.common.LANGUAGE_RESOURCE_DIRECTORY_PATH
import cn.codethink.xiaoming.common.LanguageConfiguration
import cn.codethink.xiaoming.common.PlatformSubject
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.currentTimeMillis
import cn.codethink.xiaoming.common.ensureExistedDirectory
import cn.codethink.xiaoming.configuration.LocalPlatformConfiguration
import cn.codethink.xiaoming.connection.ConnectionManagerApi
import cn.codethink.xiaoming.data.LocalPlatformData
import cn.codethink.xiaoming.internal.configuration.LocalPlatformInternalConfiguration
import cn.codethink.xiaoming.internal.event.LocalPlatformStartingEvent
import cn.codethink.xiaoming.internal.module.Module
import cn.codethink.xiaoming.internal.module.ModuleContext
import cn.codethink.xiaoming.internal.module.ModuleManagerApi
import cn.codethink.xiaoming.permission.LocalPermissionServiceApi
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.ServiceLoader
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
    val internalConfiguration: LocalPlatformInternalConfiguration,
    val logger: KLogger = KotlinLogging.logger { },
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

    private var dataNoLock: LocalPlatformData? = null
    val data: LocalPlatformData
        get() = lock.read { dataNoLock!! }

    val serializationApi = SerializationApi(this)
    val permissionServiceApi = LocalPermissionServiceApi(this)
    val moduleManagerApi = ModuleManagerApi(this)
    val connectionManagerApi = ConnectionManagerApi(this)

    private var subjectNoLock: PlatformSubject? = null
    val subject: PlatformSubject
        get() = lock.read { subjectNoLock!! }

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
        val startingEvent = LocalPlatformStartingEvent(this, cause, subject)
        val startingEventCause = EventCause(startingEvent)

        // Install modules.
        internalConfiguration.modules.forEach { installModule(it.first, startingEventCause, it.second) }
        if (internalConfiguration.findAndLoadAllModules) {
            ServiceLoader.load(Module::class.java).forEach { installModule(it, startingEventCause, XiaomingSdkSubject) }
        }

        // Set platform subject.
        subjectNoLock = PlatformSubject(internalConfiguration.id)

        // Load configuration if absent.
        if (internalConfiguration.platformConfiguration != null) {
            platformConfiguration = internalConfiguration.platformConfiguration
        } else {
            // 1. Load platform configuration.
            val configurationsDirectoryFile = File(internalConfiguration.workingDirectoryFile, "configurations")
            configurationsDirectoryFile.ensureExistedDirectory()

            val configurationFile = File(configurationsDirectoryFile, "platform.yml")
            if (!configurationFile.exists()) {
                // Copy the default configuration file.
                TODO("Copy the default configuration file.")
            }

            platformConfiguration = serializationApi.externalObjectMapper.readValue(configurationFile)

            // 2. Load language file.
            val languageFile = File(configurationsDirectoryFile, "language.yml")
            if (languageFile.isFile) {
                languageConfiguration = serializationApi.externalObjectMapper.readValue(languageFile)
            } else {
                val locale = internalConfiguration.locale
                val specifiedLocalLanguageInputStream =
                    javaClass.classLoader.getResourceAsStream("$LANGUAGE_RESOURCE_DIRECTORY_PATH/$locale.yml")
                if (specifiedLocalLanguageInputStream == null) {
                    logger.warn { "Cannot find language file for locale $locale, use default language (en_US) instead." }

                    val defaultLanguageInputStream = javaClass.classLoader.getResourceAsStream(
                        DEFAULT_LOCALE_LANGUAGE_FILE_PATH
                    ) ?: throw NoSuchElementException("Cannot find default language file.")
                    languageConfiguration = serializationApi.externalObjectMapper.readValue(defaultLanguageInputStream)
                } else {
                    languageConfiguration =
                        serializationApi.externalObjectMapper.readValue(specifiedLocalLanguageInputStream)
                }
            }
        }

        // Load data API.
        platformConfiguration.data.toDataApi(this).also { dataNoLock = LocalPlatformData(this, it) }

        // Notice modules.
        val moduleContext = ModuleContext(this, XiaomingSdkSubject, startingEventCause)
        moduleManagerApi.modules.values.forEach {
            try {
                it.value.onPlatformStarted(moduleContext)
            } catch (exception: Throwable) {
                if (internalConfiguration.failOnModuleError) {
                    throw exception
                } else {
                    logger.error(exception) { "Failed to notice module ${it.subject} that platform started." }
                }
            }
        }
    }

    private fun installModule(module: Module, cause: Cause, subject: Subject) {
        try {
            moduleManagerApi.install(module, cause, XiaomingSdkSubject)
            logger.info { "Module ${module.subject} installed from services (`findAndLoadAllModules` is true)." }
        } catch (exception: Throwable) {
            if (internalConfiguration.failOnModuleError) {
                throw exception
            } else {
                logger.error(exception) { "Failed to install module ${module.subject} from `modules`." }
            }
        }
    }

    fun stop(cause: Cause) {
        TODO()
    }

    override fun close() {
        stop(TextCause("API closed."))
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