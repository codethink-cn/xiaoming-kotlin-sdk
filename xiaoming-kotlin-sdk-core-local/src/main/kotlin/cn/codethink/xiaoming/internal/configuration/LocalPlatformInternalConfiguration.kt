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

package cn.codethink.xiaoming.internal.configuration

import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.data.LocalPlatformData
import cn.codethink.xiaoming.internal.module.Module
import cn.codethink.xiaoming.io.data.DeserializerModule
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface LocalPlatformInternalConfiguration {
    /**
     * Registry of deserializers.
     */
    val deserializerModule: DeserializerModule

    /**
     * Object mapper to serialize and deserialize internal data.
     */
    val dataObjectMapper: ObjectMapper

    /**
     * Locale of the platform.
     */
    val locale: Locale

    /**
     * Modules of the platform.
     */
    val modules: List<Module>

    /**
     * Whether to fail on module error.
     */
    val failOnModuleError: Boolean

    /**
     * Data accessing API.
     */
    val data: LocalPlatformData

    val logger: KLogger

    val subject: Subject

    val parentJob: Job?

    val parentCoroutineContext: CoroutineContext
}

data class DefaultLocalPlatformInternalConfiguration(
    override val deserializerModule: DeserializerModule,
    override val dataObjectMapper: ObjectMapper,
    override val data: LocalPlatformData,
    override val subject: Subject,
    override val locale: Locale = Locale.getDefault(),
    override val modules: List<Module> = emptyList(),
    override val failOnModuleError: Boolean = true,
    override val logger: KLogger = KotlinLogging.logger { },
    override val parentJob: Job? = null,
    override val parentCoroutineContext: CoroutineContext = EmptyCoroutineContext
) : LocalPlatformInternalConfiguration
