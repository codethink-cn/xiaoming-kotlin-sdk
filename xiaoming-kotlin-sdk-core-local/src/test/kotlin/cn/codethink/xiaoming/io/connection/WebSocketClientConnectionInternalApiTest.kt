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

package cn.codethink.xiaoming.io.connection

import cn.codethink.xiaoming.TEST_SUBJECT_DESCRIPTOR
import cn.codethink.xiaoming.common.PluginSubjectDescriptor
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.XiaomingProtocolSubject
import cn.codethink.xiaoming.common.currentTimeMillis
import cn.codethink.xiaoming.common.segmentIdOf
import cn.codethink.xiaoming.io.data.DeserializerModule
import cn.codethink.xiaoming.io.data.XiaomingJacksonModuleVersion
import cn.codethink.xiaoming.io.data.findAndApplyInitializers
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

const val TEST_HOST = "127.0.0.1"
const val TEST_PORT = 11451
const val TEST_PATH = "/1893/12/26"
const val TEST_TOKEN = "ExampleAccessToken"

class WebSocketClientConnectionInternalApiTest {
    private val logger = KotlinLogging.logger { }

    inner class TestAuthorizationService : AuthorizationService {
        override fun authorize(token: String): SubjectDescriptor? {
            if (token == TEST_TOKEN) {
                logger.info { "Authorized" }
                return XiaomingProtocolSubject
            } else {
                return null
            }
        }
    }

    private val deserializerModule = DeserializerModule(
        version = XiaomingJacksonModuleVersion,
        logger = logger
    ).apply {
        findAndApplyInitializers(javaClass.classLoader, TEST_SUBJECT_DESCRIPTOR)
    }

    private val dataObjectMapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        findAndRegisterModules()
        registerModule(deserializerModule)
    }

    @Test
    fun testConnect(): Unit = runBlocking {
        val demoPluginSubject = PluginSubjectDescriptor(segmentIdOf("cn.codethink.xiaoming.demo"))
        val supervisorJob = SupervisorJob()

        val server = LocalPlatformWebSocketServerApi(
            configuration = DefaultWebSocketServerConfiguration(
                port = TEST_PORT,
                host = TEST_HOST,
                path = TEST_PATH
            ),
            descriptor = TEST_SUBJECT_DESCRIPTOR,
            authorizationService = TestAuthorizationService(),
            parentJob = supervisorJob
        )

        val clientConfiguration = DefaultWebSocketClientConfiguration(
            host = TEST_HOST,
            port = TEST_PORT,
            path = TEST_PATH,
            token = TEST_TOKEN,
            reconnectIntervalMillis = 1000
        )
        val client = WebSocketClientConnectionInternalApi(
            configuration = clientConfiguration,
            logger = logger,
            httpClient = HttpClient { install(WebSockets) },
            descriptor = demoPluginSubject,
            parentJob = supervisorJob,
        )

        var durationMillis = currentTimeMillis
        while (true) {
            client.await(500)
            if (client.isConnected) {
                break
            }
            logger.info { "Waiting, connection.isConnected: ${client.isConnected}, pass ${currentTimeMillis - durationMillis}ms." }
        }
        durationMillis = currentTimeMillis - durationMillis

        logger.info { "connection.isConnected: ${client.isConnected}, cost ${durationMillis}ms." }

        delay(100)
        logger.info { "Closing" }
        server.close()

        logger.info { "Looking client effect" }
        delay(10000)
        client.close()

        supervisorJob.cancel()
    }

    @Test
    fun testAuthorizeFail(): Unit = runBlocking {
        val server = LocalPlatformWebSocketServerApi(
            configuration = DefaultWebSocketServerConfiguration(
                port = TEST_PORT,
                host = TEST_HOST,
                path = TEST_PATH
            ),
            descriptor = TEST_SUBJECT_DESCRIPTOR,
            authorizationService = TestAuthorizationService()
        )

        val clientConfiguration = DefaultWebSocketClientConfiguration(
            host = TEST_HOST,
            port = TEST_PORT,
            path = TEST_PATH,
            token = "WrongToken",
            reconnectIntervalMillis = 1000
        )
        val connection = WebSocketClientConnectionInternalApi(
            configuration = clientConfiguration,
            logger = logger,
            httpClient = HttpClient { install(WebSockets) },
            descriptor = TEST_SUBJECT_DESCRIPTOR
        )

        var durationMillis = currentTimeMillis
        while (true) {
            connection.await(500)
            if (connection.isConnected || (currentTimeMillis - durationMillis) > 5000) {
                break
            }
            logger.info { "Waiting, connection.isConnected: ${connection.isConnected}, pass ${currentTimeMillis - durationMillis}ms." }
        }
        durationMillis = currentTimeMillis - durationMillis

        assertFalse(connection.isConnected)
        logger.info { "connection.isConnected: ${connection.isConnected}, after ${durationMillis}ms." }

        server.close()
        connection.close()
    }
}