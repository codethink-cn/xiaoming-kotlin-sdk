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

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.HEADER_KEY_AUTHORIZATION
import cn.codethink.xiaoming.common.HEADER_VALUE_AUTHORIZATION_BEARER_WITH_SPACE
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.common.rootCause
import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.EOFException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

interface WebSocketClientConfiguration {
    val method: HttpMethod
    val host: String
    val port: Int
    val path: String
    val maxReconnectAttempts: Int?
    val reconnectIntervalMillis: Long?
    val token: String
}

val WebSocketClientConfiguration.address: String
    get() = "$host:$port$path"

data class DefaultWebSocketClientConfiguration(
    override val method: HttpMethod = HttpMethod.Get,
    override val host: String,
    override val path: String,
    override val port: Int,
    override val token: String,
    override val reconnectIntervalMillis: Long?,
    override val maxReconnectAttempts: Int? = null,
) : WebSocketClientConfiguration

class WebSocketClientConnectionInternalApi(
    private val configuration: WebSocketClientConfiguration,
    private val logger: KLogger,
    override val descriptor: SubjectDescriptor,
    httpClient: HttpClient,
    parentJob: Job? = null,
    parentCoroutineContext: CoroutineContext = Dispatchers.IO,
) : WebSocketConnectionInternalApi, CoroutineScope {
    private val supervisorJob = SupervisorJob(parentJob)
    private val scope: CoroutineScope = CoroutineScope(parentCoroutineContext + supervisorJob)
    override val coroutineContext: CoroutineContext by scope::coroutineContext

    override val channel: Channel<Frame> = Channel(Channel.UNLIMITED)

    private val lock = ReentrantReadWriteLock()
    private val condition = lock.writeLock().newCondition()

    enum class State {
        ALLOCATED,
        CONNECTING,

        CONNECTED,
        DISCONNECTED,

        WAITING,

        CLOSING,
        CLOSED,
    }

    private var stateNoLock = State.ALLOCATED
    private val state: State
        get() = lock.read { stateNoLock }

    override val isClosed: Boolean
        get() = state == State.CLOSED

    override val isConnected: Boolean
        get() = state == State.CONNECTED

    override val isConnecting: Boolean
        get() {
            val state = state
            return state == State.CONNECTING || state == State.WAITING
        }

    private var sessionNoLock: WebSocketSession? = null
    override val session: WebSocketSession
        get() = lock.read { sessionNoLock ?: throw IllegalStateException("Client is not connected.") }

    private val connectingJob = launch {
        val connectAttempts = 1..(configuration.maxReconnectAttempts ?: Int.MAX_VALUE)
        for (attempt in connectAttempts) {
            val address = configuration.address

            logger.info { "Connecting to $address with subject: $descriptor (attempt: $attempt)." }
            lock.write {
                stateNoLock = when (stateNoLock) {
                    State.ALLOCATED, State.WAITING, State.DISCONNECTED -> {
                        State.CONNECTING
                    }

                    State.CLOSING -> return@launch
                    else -> throw IllegalStateException(
                        "Client internal error: unexpected client state before connecting: $stateNoLock."
                    )
                }
                condition.signalAll()
            }

            try {
                httpClient.webSocket(
                    method = configuration.method,
                    host = configuration.host,
                    port = configuration.port,
                    path = configuration.path,
                    request = {
                        header(
                            HEADER_KEY_AUTHORIZATION,
                            HEADER_VALUE_AUTHORIZATION_BEARER_WITH_SPACE + configuration.token
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
                        condition.signalAll()
                    }
                    logger.debug { "Connected to $address." }

                    try {
                        for (frame in incoming) {
                            receive(frame)
                        }
                    } catch (_: EOFException) {
                    } catch (_: CancellationException) {
                    } catch (exception: Exception) {
                        logger.error(exception) { "Error occurred when receiving from server $address." }
                    } finally {
                        lock.write {
                            stateNoLock = when (stateNoLock) {
                                State.CONNECTED -> State.DISCONNECTED
                                State.CLOSING -> State.CLOSING
                                else -> throw IllegalStateException(
                                    "Client internal error: unexpected state after connected: $stateNoLock."
                                )
                            }
                            condition.signalAll()
                        }
                    }
                }
            } catch (exception: Exception) {
                logger.error { "Error occurred when connecting to $address: ${exception.rootCause.message}." }
            }

            // Exit if it's closing.
            lock.write {
                stateNoLock = when (stateNoLock) {
                    State.DISCONNECTED, State.CONNECTING -> State.WAITING
                    State.CLOSING, State.CLOSED -> return@launch
                    else -> throw IllegalStateException(
                        "Client internal error: unexpected state after disconnected: $stateNoLock."
                    )
                }
            }

            val reconnectIntervalMillis = configuration.reconnectIntervalMillis ?: break
            logger.info { "Disconnected from $address with subject: $descriptor (attempt: $attempt), waiting for reconnecting." }
            delay(reconnectIntervalMillis)
        }

        close(
            TextCause(
                if (configuration.reconnectIntervalMillis != null) {
                    "Max reconnect attempts reached or disconnected and reconnect is disabled."
                } else {
                    "Disconnected and reconnect is disabled."
                },
                descriptor
            )
        )
    }

    private fun assertConnected() {
        if (!isConnected) {
            throw IllegalStateException("Client is not connected, state: $state.")
        }
    }

    override fun await(time: Long, unit: TimeUnit): Boolean = lock.write {
        if (stateNoLock == State.CLOSED) {
            throw IllegalStateException("Client is closed.")
        }
        return condition.await(time, unit)
    }

    override fun await() = lock.write {
        if (stateNoLock == State.CLOSED) {
            throw IllegalStateException("Client is closed.")
        }
        condition.await()
    }

    override suspend fun send(data: Frame) {
        assertConnected()
        val session = lock.read {
            sessionNoLock ?: throw IllegalStateException(
                "Client internal error: session is not established but state is $stateNoLock."
            )
        }

        session.send(data)
        session.flush()

        logger.debug { "Sent: '$data' to ${configuration.address}." }
    }

    override suspend fun receive(data: Frame) {
        assertConnected()

        channel.send(data)
        logger.debug { "Received: '$data' from ${configuration.address}." }
    }

    override fun close() {
        close(TextCause("Client closed.", descriptor))
    }

    override fun close(cause: Cause?) = lock.write {
        stateNoLock = when (stateNoLock) {
            State.ALLOCATED, State.CONNECTING, State.CONNECTED, State.DISCONNECTED, State.WAITING -> State.CLOSING
            else -> throw IllegalStateException("Client internal error: unexpected state before closing: $stateNoLock.")
        }
        condition.signalAll()

        channel.close()
        supervisorJob.cancel()
        stateNoLock = State.CLOSED
        condition.signalAll()

        logger.debug { "Client with subject: ${this.descriptor} caused by $cause." }
    }
}