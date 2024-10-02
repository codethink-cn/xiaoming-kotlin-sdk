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

import cn.codethink.xiaoming.io.data.DeserializerModule
import cn.codethink.xiaoming.io.data.XiaomingJacksonModuleVersion
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KLogger

/**
 * Manage Jackson related APIs.
 *
 * @author Chuanwise
 */
class Serialization(
    logger: KLogger,
    objectMapper: ObjectMapper,
    deserializerModuleName: String = "DeserializerModule"
) {
    private val deserializerModule = DeserializerModule(
        name = deserializerModuleName,
        version = XiaomingJacksonModuleVersion,
        logger = logger
    )
    val deserializers = deserializerModule.deserializers
    val objectMapper = objectMapper.apply { registerModule(deserializerModule) }
}