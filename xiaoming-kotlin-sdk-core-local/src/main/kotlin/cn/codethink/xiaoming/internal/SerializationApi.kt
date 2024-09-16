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

import cn.codethink.xiaoming.common.DefaultMapRegistrations
import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.io.LocalPlatformModule
import cn.codethink.xiaoming.io.data.PlatformAnnotationIntrospector
import cn.codethink.xiaoming.io.data.PlatformModule
import com.fasterxml.jackson.databind.AnnotationIntrospector
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector

/**
 * Manage Jackson related APIs.
 *
 * @author Chuanwise
 */
class SerializationApi(
    val internalApi: LocalPlatformInternalApi
) {
    private val jacksonModules = DefaultMapRegistrations<Class<out Module>, Module>()

    val platformModule = PlatformModule()
    val localPlatformModule = LocalPlatformModule()

    val dataObjectMapper: ObjectMapper = internalApi.internalConfiguration.dataObjectMapper.apply { initialize() }
    val configurationObjectMapper: ObjectMapper =
        internalApi.internalConfiguration.configurationObjectMapper.apply { initialize() }

    private fun ObjectMapper.initialize() {
        setAnnotationIntrospector(
            AnnotationIntrospector.pair(
                JacksonAnnotationIntrospector(),
                PlatformAnnotationIntrospector()
            )
        )

        findAndRegisterModules()
        registerModule(platformModule)
        registerModule(localPlatformModule)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Module> getJacksonModuleByType(type: Class<T>): T? = jacksonModules.get(type) as T?
    operator fun <T : Module> get(type: Class<T>) = getJacksonModuleByType(type)

    fun registerJacksonModule(module: Module, subject: Subject) {
        jacksonModules.register(module.javaClass, DefaultRegistration(module, subject))
        dataObjectMapper.registerModule(module)
        configurationObjectMapper.registerModule(module)
    }
}