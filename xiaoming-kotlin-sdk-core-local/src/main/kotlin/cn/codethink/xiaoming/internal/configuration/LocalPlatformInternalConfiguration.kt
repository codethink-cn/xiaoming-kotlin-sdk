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

import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.data.LocalPlatformData
import cn.codethink.xiaoming.internal.Serialization
import cn.codethink.xiaoming.internal.module.Module
import java.util.Locale

interface LocalPlatformInternalConfiguration {
    val id: Id

    /**
     * Serialization related APIs.
     */
    val serialization: Serialization

    /**
     * Locale of the platform.
     */
    val locale: Locale

    /**
     * Data accessing API.
     */
    val data: LocalPlatformData

    /**
     * Modules to install.
     */
    val modules: List<Module>

    /**
     * Whether to fail on module error.
     */
    val failOnModuleError: Boolean
}

data class DefaultLocalPlatformInternalConfiguration(
    override val id: Id,
    override val serialization: Serialization,
    override val locale: Locale,
    override val data: LocalPlatformData,
    override val modules: List<Module> = emptyList(),
    override val failOnModuleError: Boolean = true
) : LocalPlatformInternalConfiguration
