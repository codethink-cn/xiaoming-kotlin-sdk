/*
 * Copyright 2025 CodeThink Technologies and contributors.
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

package cn.codethink.xiaoming.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper

class SerializationManagerImpl(
    override val codecResolver: CodecResolver,
    private val findAndRegisterModules: Boolean
) : SerializationManager {
    override val jsonFileObjectMapper: ObjectMapper = createJsonObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
    }
    override val jsonDataObjectMapper: ObjectMapper = createJsonObjectMapper()

    override val yamlFileObjectMapper: ObjectMapper = createYamlObjectMapper()

    private fun createJsonObjectMapper(): ObjectMapper {
        return ObjectMapper().initialized()
    }

    private fun createYamlObjectMapper(): ObjectMapper {
        return YAMLMapper.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .build()
            .initialized()
    }

    private fun ObjectMapper.initialized(): ObjectMapper = apply {
        if (findAndRegisterModules) {
            findAndRegisterModules()
        }
        registerModule(codecResolver.asJacksonModule())
    }
}