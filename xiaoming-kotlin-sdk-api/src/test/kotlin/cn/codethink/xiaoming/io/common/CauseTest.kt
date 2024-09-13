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

package cn.codethink.xiaoming.io.common

import cn.codethink.xiaoming.common.PacketIdCause
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.common.assertJsonContentEquals
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test

class CauseTest {
    @Test
    fun testSerializeCause() {
        val mapper = jacksonObjectMapper().apply {
            enable(SerializationFeature.INDENT_OUTPUT)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }

        val textCause = TextCause("Reason message here.")
        val packetCause = PacketIdCause("packet-id")

        mapper.assertJsonContentEquals(
            """
            {
              "type": "text",
              "text": "Reason message here."
            }`
        """.trimIndent(), textCause
        )

        mapper.assertJsonContentEquals(
            """
            {
              "type": "packet-id",
              "id": "packet-id"
            }
        """.trimIndent(), packetCause
        )
    }
}