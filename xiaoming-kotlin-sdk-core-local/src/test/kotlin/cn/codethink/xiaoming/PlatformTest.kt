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

import cn.codethink.xiaoming.common.ConnectionSubjectDescriptor
import cn.codethink.xiaoming.common.TestSubjectDescriptor
import cn.codethink.xiaoming.common.toId
import cn.codethink.xiaoming.io.connection.DefaultWebSocketClientConfiguration
import cn.codethink.xiaoming.io.connection.TextFrameConnectionApi
import cn.codethink.xiaoming.io.connection.WebSocketClientConnectionInternalApi
import cn.codethink.xiaoming.io.data.DeserializerModule
import cn.codethink.xiaoming.io.data.JacksonModuleVersion
import cn.codethink.xiaoming.io.data.findAndApplyInitializers
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import org.junit.jupiter.api.Test

const val TEST_PATH = "/"
const val TEST_PORT = 8080
const val TEST_TOKEN = "ExampleAccessToken"

class PlatformTest {
    private val logger = KotlinLogging.logger { }

    private val deserializerModule = DeserializerModule(
        version = JacksonModuleVersion,
        logger = logger
    ).apply {
        findAndApplyInitializers(javaClass.classLoader, TestSubjectDescriptor)
    }

    private val dataObjectMapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        findAndRegisterModules()
        registerModule(deserializerModule)
    }

    @Test
    fun testConnectAsPlugin() {
        val subject = ConnectionSubjectDescriptor("demo-client".toId())
        val connectionInternalApi = WebSocketClientConnectionInternalApi(
            configuration = DefaultWebSocketClientConfiguration(
                host = "localhost",
                path = TEST_PATH,
                port = TEST_PORT,
                token = TEST_TOKEN,
                reconnectIntervalMillis = null
            ),
            logger = logger,
            descriptor = subject,
            httpClient = HttpClient { install(WebSockets) }
        )

        val connectionApi = TextFrameConnectionApi(
            logger = logger,
            objectMapper = dataObjectMapper,
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
//                ).apply { start(TEST_CAUSE, TEST_SUBJECT_DESCRIPTOR) }
//            ),
//            subject = subject,
//            connectionApi = connectionApi
//        )
    }
}