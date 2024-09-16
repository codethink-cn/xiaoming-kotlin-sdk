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
import cn.codethink.xiaoming.internal.module.Module
import cn.codethink.xiaoming.permission.data.LocalPlatformConfiguration
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

/**
 * Effect configuration for local platform.
 *
 * @author Chuanwise
 */
data class LocalPlatformInternalConfiguration(
    val workingDirectoryFile: File,
    val modulesToInstall: List<Pair<Module, Subject>> = emptyList(),
    val platformConfiguration: LocalPlatformConfiguration? = null,
    val dataObjectMapper: ObjectMapper = jacksonObjectMapper(),
    val configurationObjectMapper: ObjectMapper = YAMLMapper.builder().build(),
    var findAndLoadAllModules: Boolean = true,
    var findAndLoadAllJacksonModules: Boolean = true,
    var failOnModuleError: Boolean = true
)