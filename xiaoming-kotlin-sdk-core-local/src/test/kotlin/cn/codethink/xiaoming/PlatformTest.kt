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

package cn.codethink.xiaoming

import cn.codethink.xiaoming.common.ConnectionSubject
import cn.codethink.xiaoming.common.PlatformSubject
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.getTestResourceAsStream
import cn.codethink.xiaoming.common.toId
import cn.codethink.xiaoming.data.LocalPlatformData
import cn.codethink.xiaoming.data.LocalPlatformDataConfiguration
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.internal.Serialization
import cn.codethink.xiaoming.internal.configuration.DefaultLocalPlatformInternalConfiguration
import cn.codethink.xiaoming.io.connection.DefaultWebSocketClientConfiguration
import cn.codethink.xiaoming.io.connection.TextFrameConnectionApi
import cn.codethink.xiaoming.io.connection.WebSocketClientConnectionInternalApi
import cn.codethink.xiaoming.io.data.registerPlatformDeserializers
import cn.codethink.xiaoming.io.registerLocalDataSqlDeserializers
import cn.codethink.xiaoming.io.registerLocalPlatformDeserializers
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import org.junit.jupiter.api.Test
import java.util.Locale

const val TEST_PATH = "/"
const val TEST_PORT = 8080
const val TEST_TOKEN = "ExampleAccessToken"

val TEST_CAUSE = TextCause("Run test programs")
val TEST_SUBJECT = XiaomingSdkSubject

class PlatformTest {
    companion object {
        val logger = KotlinLogging.logger { }

        val serialization = Serialization(
            logger = logger,
            objectMapper = jacksonObjectMapper().apply {
                propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
                findAndRegisterModules()
            }
        ).apply {
            deserializers.registerPlatformDeserializers(TEST_SUBJECT)
            deserializers.registerLocalPlatformDeserializers(TEST_SUBJECT)
            deserializers.registerLocalDataSqlDeserializers(TEST_SUBJECT)
        }

        val api = LocalPlatformInternalApi(
            logger = logger,
            configuration = DefaultLocalPlatformInternalConfiguration(
                id = "Test".toId(),
                serialization = serialization,
                locale = Locale.getDefault(),
                data = LocalPlatformData(
                    platformDataApi = getTestResourceAsStream("xiaoming/data.json").use {
                        serialization.objectMapper.readValue(it, LocalPlatformDataConfiguration::class.java)
                    }.toDataApi(serialization)
                )
            ),
            subject = PlatformSubject("test".toId())
        ).apply {
            start(TEST_CAUSE, TEST_SUBJECT)
        }
    }

    @Test
    fun testConnectAsPlugin() {
        val subject = ConnectionSubject("demo-client".toId())
        val connectionInternalApi = WebSocketClientConnectionInternalApi(
            configuration = DefaultWebSocketClientConfiguration(
                host = "localhost",
                path = TEST_PATH,
                port = TEST_PORT,
                token = TEST_TOKEN,
                reconnectIntervalMillis = null
            ),
            logger = logger,
            subject = subject,
            httpClient = HttpClient { install(WebSockets) }
        )

        val connectionApi = TextFrameConnectionApi(
            logger = logger,
            objectMapper = serialization.objectMapper,
            connectionInternalApi = connectionInternalApi
        )

//        platformInternalApi.serializationApi.externalObjectMapper
//        val packetConnection = PacketConnection(
//            logger = logger,
//            session = randomUuidString(),
//            platform = Platform(
//                api = LocalPlatformApi(
//                    platformInternalApi = platformInternalApi,
//                    language =
//                ).apply { start(TEST_CAUSE, TEST_SUBJECT) }
//            ),
//            subject = subject,
//            connectionApi = connectionApi
//        )
    }
}