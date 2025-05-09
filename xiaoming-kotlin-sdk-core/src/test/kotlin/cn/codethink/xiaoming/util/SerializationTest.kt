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

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.serialization.CodecResolverImpl
import cn.codethink.xiaoming.serialization.findAndApplyInitializers
import cn.codethink.xiaoming.serialization.registering
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test

@OptIn(InternalApi::class)
class SerializationTest {
    private val operation = Operation("Just for Test", TestSubjectDescriptor)
    private val codecResolver = CodecResolverImpl().apply {
        findAndApplyInitializers(operation)
    }
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(codecResolver.asJacksonModule())
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    private inline fun <reified T> ObjectMapper.assertEquals(string: String, value: T) {
        val serialized = writeValueAsString(value)
        val deserialized = readValue(string, T::class.java)

        assertContentEquals(serialized, value)
        assertContentEquals(string, deserialized)
    }

    @Test
    fun testCause() {
        objectMapper.assertEquals(
            """
            {
              "description" : "Reason message here."
            }
            """.trimIndent(), Cause("Reason message here.")
        )
    }

    @Test
    fun testVersion() {
        objectMapper.assertEquals("1.0.0".toDoubleQuotedString(), Version("1.0.0"))
        objectMapper.assertEquals("1.2.0-SNAPSHOT".toDoubleQuotedString(), Version("1.2.0-SNAPSHOT"))
    }

    @Test
    fun testVersionPattern() {
        objectMapper.assertEquals(">=0.1.0".toDoubleQuotedString(), VersionPattern(">=0.1.0"))
        objectMapper.assertEquals(">=0.1.0-SNAPSHOT".toDoubleQuotedString(), VersionPattern(">=0.1.0-SNAPSHOT"))
    }

    interface Config
    interface ConfigSon : Config

    class ConfigSonV1(val value: String) : ConfigSon
    class ConfigSonV2(val field: String) : ConfigSon
    class ConfigSonV3(val config: ConfigSon) : ConfigSon

    init {
        codecResolver.registering(operation) {
            type<Config> {
                type {
                    type<ConfigSon>("son") {
                        version {
                            hint<ConfigSonV1>("1")
                            hint<ConfigSonV2>("2")
                            hint<ConfigSonV3>("3")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testConfig() {
        val configWriter = objectMapper

        val configSonV1 = ConfigSonV1("114")
        objectMapper.assertJsonStringContentEquals(
            """
                {
                    "type": "son",
                    "version": "1",
                    "value": "114"
                }
            """.trimIndent(), configWriter.writeValueAsString(configSonV1)
        )

        val configSonV2 = ConfigSonV2("114514")
        objectMapper.assertJsonStringContentEquals(
            """
                {
                    "type": "son",
                    "version": "2",
                    "field": "114514"
                }
            """.trimIndent(), configWriter.writeValueAsString(configSonV2)
        )
    }

    @Test
    fun testConfigV3() {
        val configSonV1 = ConfigSonV1("114")
        val configSonV3 = ConfigSonV3(configSonV1)
        objectMapper.assertJsonStringContentEquals(
            """
                {
                    "type": "son",
                    "version": "3",
                    "config": {
                        "version": "1",
                        "value": "114"
                    }
                }
            """.trimIndent(), objectMapper.writeValueAsString(configSonV3)
        )
    }
}