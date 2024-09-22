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

import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.XiaomingProtocolSubject
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.currentTimeMillis
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpMethod
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

const val TEST_HOST = "127.0.0.1"
const val TEST_PORT = 11451
const val TEST_PATH = "/1893/12/26"
const val TEST_TOKEN = "ExampleAccessToken"

class WebSocketConnectionTest {
    val logger = KotlinLogging.logger { }

    data class TestWebSocketConnectionConfiguration(
        override val method: HttpMethod = HttpMethod.Get,
        override val host: String = TEST_HOST,
        override val port: Int = TEST_PORT,
        override val path: String = TEST_PATH,
        override val maxReconnectAttempts: Int? = null,
        override val token: String = TEST_TOKEN,
        override val reconnectIntervalMillis: Long? = 5000
    ) : WebSocketConnectionConfiguration

    data class TestLocalPlatformWebSocketConnectionsConfiguration(
        override val port: Int = TEST_PORT,
        override val host: String? = TEST_HOST,
        override val path: String = TEST_PATH
    ) : LocalPlatformWebSocketConnectionsConfiguration

    inner class TestAuthorizer : Authorizer {
        override fun authorize(token: String): Subject? {
            if (token == TEST_TOKEN) {
                logger.info { "Authorized" }
                return XiaomingProtocolSubject
            } else {
                return null
            }
        }
    }

    @Test
    fun testConnect(): Unit = runBlocking {
        val supervisorJob = SupervisorJob()
        val subject = XiaomingSdkSubject

        val connections = LocalPlatformWebSocketConnections(
            configuration = TestLocalPlatformWebSocketConnectionsConfiguration(),
            logger = logger,
            subject = subject,
            authorizer = TestAuthorizer(),
            parentJob = supervisorJob
        )

        val connectionConfiguration = TestWebSocketConnectionConfiguration()
        val connection = WebSocketConnection(
            configuration = connectionConfiguration,
            logger = logger,
            httpClient = HttpClient { install(WebSockets) },
            subject = subject,
            parentJob = supervisorJob
        )

        var durationMillis = currentTimeMillis
        while (true) {
            connection.await(500)
            if (connection.isConnected) {
                break
            }
            logger.info { "Waiting, connection.isConnected: ${connection.isConnected}, pass ${currentTimeMillis - durationMillis}ms." }
        }
        durationMillis = currentTimeMillis - durationMillis

        logger.info { "connection.isConnected: ${connection.isConnected}, cost ${durationMillis}ms." }

        delay(100)
        logger.info { "Closing" }
        connections.close()

        logger.info { "Looking client effect" }
        delay(10000)
        connection.close()

        supervisorJob.cancel()
    }

    @Test
    fun testAuthorizeFail(): Unit = runBlocking {
        val supervisorJob = SupervisorJob()
        val subject = XiaomingSdkSubject

        val connections = LocalPlatformWebSocketConnections(
            configuration = TestLocalPlatformWebSocketConnectionsConfiguration(),
            logger = logger,
            subject = subject,
            authorizer = TestAuthorizer(),
            parentJob = supervisorJob
        )

        val connectionConfiguration = TestWebSocketConnectionConfiguration(
            token = "WrongToken",
            reconnectIntervalMillis = 1000
        )
        val connection = WebSocketConnection(
            configuration = connectionConfiguration,
            logger = logger,
            httpClient = HttpClient { install(WebSockets) },
            subject = subject,
            parentJob = supervisorJob
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

        connections.close()
        connection.close()
        supervisorJob.cancel()
    }
}