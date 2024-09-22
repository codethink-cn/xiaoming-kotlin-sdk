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

import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.common.XiaomingProtocolSubject
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.currentTimeMillis
import cn.codethink.xiaoming.common.segmentIdOf
import cn.codethink.xiaoming.common.toId
import cn.codethink.xiaoming.common.toLiteralMatcher
import cn.codethink.xiaoming.data.insertAndGetPermissionProfile
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.internal.configuration.LocalPlatformInternalConfiguration
import cn.codethink.xiaoming.io.data.PlatformAnnotationIntrospector
import com.fasterxml.jackson.databind.AnnotationIntrospector
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpMethod
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

const val TEST_HOST = "127.0.0.1"
const val TEST_PORT = 11451
const val TEST_PATH = "/1893/12/26"
const val TEST_TOKEN = "ExampleAccessToken"

class WebSocketClientTest {
    val logger = KotlinLogging.logger { }

    data class TestWebSocketClientConfiguration(
        override val method: HttpMethod = HttpMethod.Get,
        override val host: String = TEST_HOST,
        override val port: Int = TEST_PORT,
        override val path: String = TEST_PATH,
        override val maxReconnectAttempts: Int? = null,
        override val token: String = TEST_TOKEN,
        override val reconnectIntervalMillis: Long? = 5000
    ) : WebSocketClientConfiguration

    data class TestLocalPlatformWebSocketServerConfiguration(
        override val port: Int = TEST_PORT,
        override val host: String? = TEST_HOST,
        override val path: String = TEST_PATH
    ) : WebSocketServerConfiguration

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

    companion object {
        val logger = KotlinLogging.logger { }
        val mapper = jacksonObjectMapper().apply {
            setAnnotationIntrospector(
                AnnotationIntrospector.pair(
                    PlatformAnnotationIntrospector(),
                    JacksonAnnotationIntrospector()
                )
            )
            findAndRegisterModules()
        }

        val internalConfiguration = LocalPlatformInternalConfiguration(
            workingDirectoryFile = File("platform"),
            id = "Test".toId()
        )
        val api = LocalPlatformInternalApi(internalConfiguration, logger).apply {
            start(TextCause("Run test programs"), XiaomingSdkSubject)
        }

        val subject = PluginSubject(segmentIdOf("cn.codethink.xiaoming.demo"))
        val subjectMatcher = subject.toLiteralMatcher()

        val profile = api.data.insertAndGetPermissionProfile(subject)
    }

    @Test
    fun testConnect(): Unit = runBlocking {
        val supervisorJob = SupervisorJob()
        val subject = XiaomingSdkSubject

        val server = LocalPlatformWebSocketServer(
            configuration = TestLocalPlatformWebSocketServerConfiguration(),
            internalApi = api,
            subject = subject,
            authorizer = TestAuthorizer(),
            parentJob = supervisorJob
        )

        val clientConfiguration = TestWebSocketClientConfiguration()
        val client = WebSocketClient(
            configuration = clientConfiguration,
            logger = logger,
            httpClient = HttpClient { install(WebSockets) },
            subject = subject,
            parentJob = supervisorJob
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
        val supervisorJob = SupervisorJob()
        val subject = XiaomingSdkSubject

        val server = LocalPlatformWebSocketServer(
            configuration = TestLocalPlatformWebSocketServerConfiguration(),
            internalApi = api,
            subject = subject,
            authorizer = TestAuthorizer(),
            parentJob = supervisorJob
        )

        val clientConfiguration = TestWebSocketClientConfiguration(
            token = "WrongToken",
            reconnectIntervalMillis = 1000
        )
        val connection = WebSocketClient(
            configuration = clientConfiguration,
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

        server.close()
        connection.close()
        supervisorJob.cancel()
    }
}