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

import cn.codethink.xiaoming.common.HEADER_KEY_AUTHORIZATION
import cn.codethink.xiaoming.common.HEADER_VALUE_AUTHORIZATION_BEARER_WITH_SPACE
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.io.data.Packet
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

interface WebSocketConnectionConfiguration {
    val method: HttpMethod
    val host: String
    val port: Int
    val path: String
    val maxReconnectAttempts: Int?
    val accessToken: String
}

val WebSocketConnectionConfiguration.address: String
    get() = "$host:$port$path"


class WebSocketConnection(
    private val configuration: WebSocketConnectionConfiguration,
    private val mapper: ObjectMapper,
    private val logger: KLogger,
    httpClient: HttpClient,
    override val subject: Subject,
    parentJob: Job? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WebSocketLikeConnection, CoroutineScope {
    private val supervisorJob = SupervisorJob(parentJob)
    private val scope: CoroutineScope = CoroutineScope(dispatcher + supervisorJob)
    override val coroutineContext: CoroutineContext by scope::coroutineContext

    private val channel: Channel<Packet> = Channel(Channel.UNLIMITED)
    private val lock = ReentrantReadWriteLock()

    enum class State {
        INITIALIZED,
        CONNECTING,

        CONNECTED,
        DISCONNECTED,

        WAITING,

        CLOSING,
        CLOSED,
    }

    private var stateNoLock = State.INITIALIZED
    private val state: State
        get() = lock.read { stateNoLock }

    override val isClosed: Boolean
        get() = state == State.CLOSED

    override val isConnected: Boolean
        get() = state == State.CONNECTED

    private var session: WebSocketSession? = null
    private val connectingJob = launch {
        val connectAttempts = 1..(configuration.maxReconnectAttempts ?: Int.MAX_VALUE)
        for (attempt in connectAttempts) {
            val address = configuration.address

            logger.info { "Connecting to $address with subject: $subject (attempt: $attempt)." }
            lock.write {
                stateNoLock = when (stateNoLock) {
                    State.INITIALIZED, State.WAITING -> {
                        State.CONNECTING
                    }

                    State.CLOSING -> return@launch
                    else -> throw IllegalStateException(
                        "Client internal error: unexpected client state before connecting: $stateNoLock."
                    )
                }

                httpClient.webSocket(
                    method = configuration.method,
                    host = configuration.host,
                    port = configuration.port,
                    path = configuration.path,
                    request = {
                        header(
                            HEADER_KEY_AUTHORIZATION,
                            HEADER_VALUE_AUTHORIZATION_BEARER_WITH_SPACE + configuration.accessToken
                        )
                    }
                ) {
                    lock.write {
                        stateNoLock = when (stateNoLock) {
                            State.CONNECTING -> State.CONNECTED
                            State.CLOSING -> return@webSocket
                            else -> throw IllegalStateException(
                                "Client internal error: unexpected client state after connected: $stateNoLock."
                            )
                        }
                    }
                    logger.debug { "Handshaking with $address." }

                    try {
                        for (frame in incoming) {
                            if (frame !is Frame.Text) {
                                logger.warn { "Unexpected incoming frame: $frame from $address, except `Frame.Text`!" }
                                continue
                            }

                            val text = frame.readText()
                            receive(text)
                        }
                    } catch (exception: Exception) {
                        logger.error(exception) { "Error occurred when receiving from $address." }

                    } finally {
                        lock.write {
                            stateNoLock = when (stateNoLock) {
                                State.CONNECTED -> State.DISCONNECTED
                                State.CLOSING -> State.CLOSING
                                else -> throw IllegalStateException(
                                    "Client internal error: unexpected state after connected: $stateNoLock."
                                )
                            }
                        }
                    }
                }
            }

            // Exit if it's closing.
            lock.read {
                if (stateNoLock == State.CLOSING) {
                    return@launch
                }
            }
        }
    }

    private fun assertConnected() {
        if (!isConnected) {
            throw IllegalStateException("Client is not connected, state: $state.")
        }
    }

    override suspend fun send(string: String) = lock.read {
        assertConnected()

        val session = session ?: throw IllegalStateException(
            "Client internal error: session is not established but state is $stateNoLock."
        )

        val frame = Frame.Text(string)
        session.outgoing.send(frame)
        logger.debug { "Sent: $string to ${configuration.address}." }
    }

    override suspend fun receive(string: String) = lock.read {
        assertConnected()

        val packet = mapper.readValue<Packet>(string)
        channel.send(packet)
    }

    override fun close(): Unit = runBlocking {
        lock.write {
            stateNoLock = when (stateNoLock) {
                State.INITIALIZED, State.CONNECTING, State.CONNECTED, State.DISCONNECTED, State.WAITING -> State.CLOSING
                else -> throw IllegalStateException("Client internal error: unexpected state before closing: $stateNoLock.")
            }

            channel.close()
            supervisorJob.cancelAndJoin()
            stateNoLock = State.CLOSED

            logger.debug { "Closed client with subject: $subject." }
        }
    }
}