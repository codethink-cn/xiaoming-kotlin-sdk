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

package cn.codethink.xiaoming.io.data

import cn.codethink.xiaoming.common.PluginSubjectDescriptor
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.assertJsonContentEquals
import cn.codethink.xiaoming.io.packet.Packet
import cn.codethink.xiaoming.io.packet.RequestPacket
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DataTest {
    private var RequestPacket.extensionField1: String
        get() = raw["cn.chuanwise.xiaoming:extension-field-1"]
        set(value) = raw.set("cn.chuanwise.xiaoming:extension-field-1", value)

    private var RequestPacket.extensionField2Map: Map<String, String>
        get() = raw["cn.chuanwise.xiaoming:extension-field-2"]
        set(value) = raw.set("cn.chuanwise.xiaoming:extension-field-2", value)

    private data class Birthday(val birthday: String)

    private val RequestPacket.extensionField2Birthday: Birthday
        get() = raw["cn.chuanwise.xiaoming:extension-field-2"]

    @Test
    fun testDeserializeData() {
        val deserializerModule = DeserializerModule().apply {
            findAndApplyInitializers(javaClass.classLoader, XiaomingSdkSubject)
        }
        val mapper = jacksonObjectMapper().apply {
            registerModule(deserializerModule)
            configure(SerializationFeature.INDENT_OUTPUT, true)
        }
        val packet = mapper.readValue<Packet>(
            """
            {
              "id": "c4d038b4-bed0-3fdb-9471-ef51ec3a32cd",
              "type": "request",
              "action": "api-name",
              "subject": {
                "id": "cn.codethink:chat-commands",
                "type": "plugin"
              },
              "cn.chuanwise.xiaoming:extension-field-1": "extension-field-1-value",
              "cn.chuanwise.xiaoming:extension-field-2": {
                "birthday": "1893-12-26"
              }
            }
            """.trimMargin()
        ) as RequestPacket

        // Deserialized from string.
        Assertions.assertEquals("c4d038b4-bed0-3fdb-9471-ef51ec3a32cd", packet.id)
        Assertions.assertEquals(PluginSubjectDescriptor(NodeRaw(mapper).apply {
            set("id", "cn.codethink:chat-commands")
            set("type", "plugin")
        }), packet.subjectDescriptor)

        // Extension field 1 read and write.
        Assertions.assertEquals("extension-field-1-value", packet.extensionField1)
        packet.extensionField1 = "extension-field-1-value-modified"
        Assertions.assertEquals("extension-field-1-value-modified", packet.extensionField1)

        // Multi-class extension field read and write.
        Assertions.assertEquals(mapOf("birthday" to "1893-12-26"), packet.extensionField2Map)
        Assertions.assertEquals(Birthday("1893-12-26"), packet.extensionField2Birthday)

        packet.extensionField2Map = mapOf("birthday" to "1893-12-26-modified")
        Assertions.assertEquals(mapOf("birthday" to "1893-12-26-modified"), packet.extensionField2Map)
        Assertions.assertEquals(Birthday("1893-12-26-modified"), packet.extensionField2Birthday)

        mapper.assertJsonContentEquals(
            """
                {
                  "id" : "c4d038b4-bed0-3fdb-9471-ef51ec3a32cd",
                  "type" : "request",
                  "action" : "api-name",
                  "subject" : {
                    "id" : "cn.codethink:chat-commands",
                    "type" : "plugin"
                  },
                  "cn.chuanwise.xiaoming:extension-field-1" : "extension-field-1-value-modified",
                  "cn.chuanwise.xiaoming:extension-field-2" : {
                    "birthday" : "1893-12-26-modified"
                  }
                }
            """.trimIndent(), packet
        )
    }
}