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
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.io.data.DeserializerModule
import cn.codethink.xiaoming.io.data.PlatformAnnotationIntrospector
import cn.codethink.xiaoming.io.data.XiaomingJacksonModuleVersion
import cn.codethink.xiaoming.io.data.registerPlatformDeserializers
import cn.codethink.xiaoming.io.registerLocalPlatformDeserializers
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
    private val deserializerModule = DeserializerModule(
        name = "DeserializerModule-${internalApi.internalConfiguration.id}",
        version = XiaomingJacksonModuleVersion,
        logger = internalApi.logger
    )
    val polymorphicDeserializers = deserializerModule.deserializers

    private val jacksonModules = DefaultMapRegistrations<Class<out Module>, Module>()

    val internalObjectMapper: ObjectMapper =
        internalApi.internalConfiguration.internalObjectMapper.apply { initialize() }
    val externalObjectMapper: ObjectMapper =
        internalApi.internalConfiguration.externalObjectMapper.apply { initialize() }

    init {
        polymorphicDeserializers.registerPlatformDeserializers(XiaomingSdkSubject)
        polymorphicDeserializers.registerLocalPlatformDeserializers(XiaomingSdkSubject)
    }

    private fun ObjectMapper.initialize() {
        setAnnotationIntrospector(
            AnnotationIntrospector.pair(
                JacksonAnnotationIntrospector(),
                PlatformAnnotationIntrospector()
            )
        )

        findAndRegisterModules()
        registerModule(deserializerModule)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Module> getJacksonModuleByType(type: Class<T>): T? = jacksonModules[type] as T?
    operator fun <T : Module> get(type: Class<T>) = getJacksonModuleByType(type)

    fun registerJacksonModule(module: Module, subject: Subject) {
        jacksonModules.register(module.javaClass, DefaultRegistration(module, subject))
        internalObjectMapper.registerModule(module)
        externalObjectMapper.registerModule(module)
    }
}