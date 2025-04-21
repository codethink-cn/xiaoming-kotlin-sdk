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

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.util.Received
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.ConnectionDescriptor
import cn.codethink.xiaoming.util.Subject
import cn.codethink.xiaoming.util.throwConnectionAlreadyClosedException
import cn.codethink.xiaoming.util.throwConnectionNotConnectedException
import cn.codethink.xiaoming.util.throwConnectionNotStartedYetException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.EOFException
import java.net.ConnectException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

class AutoReconnectSupportedWebSocketClientImpl(
    private val host: String,
    private val port: Int,
    private val path: String,
    private val autoReconnectPolicy: AutoReconnectPolicy,
    override val descriptor: ConnectionDescriptor,
    private val httpMethod: HttpMethod = HttpMethod.Get,
    httpClient: HttpClient? = null,
    manageHttpClient: Boolean? = null,
    initialPaused: Boolean = false,
    parentJob: Job? = null,
    parentCoroutineContext: CoroutineContext = Dispatchers.IO,
    private val httpRequestBuilderProcessor: HttpRequestBuilder.() -> Unit = {}
) : AutoReconnectSupportedClient<Frame> {
    private val job = SupervisorJob(parentJob)
    private val scope = CoroutineScope(job + parentCoroutineContext)
    override val coroutineContext: CoroutineContext get() = scope.coroutineContext

    private val manageHttpClient = manageHttpClient ?: (httpClient == null)
    private val httpClient = httpClient ?: HttpClient { install(WebSockets) }

    private sealed interface State
    private object CreatedState : State
    private object WaitingState : State
    private object ClosingState : State
    private object ClosedState : State
    private object PausedState : State
    private object ConnectingState : State

    private class ConnectedState(
        val session: ClientWebSocketSession
    ) : State {
        val incoming = Channel<Received<Frame>>(Channel.UNLIMITED)
    }

    private val stateFlow = MutableStateFlow<State>(
        CreatedState
    )
    private val paused = AtomicBoolean(initialPaused)

    override val isConnected: Boolean get() = stateFlow.value is ConnectedState
    override val isConnecting: Boolean get() = stateFlow.value is ConnectingState

    override val isPaused: Boolean
        get() = when (stateFlow.value) {
            ClosedState, ClosingState -> throwConnectionAlreadyClosedException()
            CreatedState -> throwConnectionNotStartedYetException()
            PausedState -> true
            WaitingState -> false
            else -> paused.get()
        }

    override val isClosed: Boolean get() = stateFlow.value == ClosedState
    override val isClosing: Boolean get() = stateFlow.value == ClosingState

    override val isClosingOrClosed: Boolean
        get() = when (stateFlow.value) {
            ClosedState, ClosingState -> true
            else -> false
        }

    private val session: WebSocketSession
        get() = when (val state = stateFlow.value) {
            is ConnectedState -> state.session
            else -> throwConnectionNotConnectedException()
        }

    override val incoming: ReceiveChannel<Received<Frame>>
        get() = when (val state = stateFlow.value) {
            is ConnectedState -> state.incoming
            else -> throwConnectionNotConnectedException()
        }

    override fun start() {
        stateFlow.update {
            when (it) {
                CreatedState -> ConnectingState
                else -> error("Unexpected state: $it. You can only call `start()` once before calling `close()`.")
            }
        }
        createConnectingJob()
    }

    override fun ensureStarted() {
        stateFlow.update {
            when (it) {
                CreatedState -> ConnectingState
                else -> return
            }
        }
        createConnectingJob()
    }

    private fun createConnectingJob(): Job = launch {
        var attempt = 0
        do {
            stateFlow.updateAndGet {
                when (it) {
                    ClosedState, ClosingState -> return@launch
                    is ConnectingState -> it
                    PausedState -> return@launch
                    WaitingState -> ConnectingState
                    else -> error("Unexpected state: $it before connecting.")
                }
            }

            try {
                val session = httpClient.webSocketSession(httpMethod, host, port, path, httpRequestBuilderProcessor)
                val state =
                    ConnectedState(session)

                attempt = 0

                stateFlow.update {
                    when (it) {
                        ClosedState, ClosingState -> {
                            session.close()
                            return@launch
                        }

                        is ConnectingState -> {
                            state
                        }

                        else -> error("Unexpected state: $it after connected.")
                    }
                }

                for (frame in session.incoming) {
                    state.incoming.send(Received(frame))
                }
            } catch (e: Throwable) {
                when (e) {
                    is EOFException -> Unit
                    is ConnectException -> Unit
                    is CancellationException -> Unit
                    else -> Unit
                }
            }

            var delay: Duration? = null
            stateFlow.update {
                when (it) {
                    ClosedState, ClosingState -> return@launch
                    ConnectingState, is ConnectedState -> {
                        if (isPaused) {
                            PausedState
                        } else {
                            delay = autoReconnectPolicy.nextDelayOrNull(attempt)
                            if (delay == null) {
                                PausedState
                            } else {
                                WaitingState
                            }
                        }
                    }

                    else -> error("Unexpected state: $it after disconnected.")
                }
            }
            val delayFinal = delay ?: return@launch

            withTimeoutOrNull(delayFinal.inWholeMilliseconds) {
                stateFlow.collect()
            }
        } while (true)
    }

    override fun pause() {
        stateFlow.update {
            when (it) {
                ClosedState, ClosingState -> throwConnectionAlreadyClosedException()
                ConnectingState, is ConnectedState -> {
                    paused.set(true)
                    return
                }

                CreatedState -> throwConnectionNotStartedYetException()
                PausedState -> error("The connection is already paused.")
                WaitingState -> PausedState
            }
        }
    }

    override fun ensurePaused() {
        stateFlow.update {
            when (it) {
                ClosedState, ClosingState -> throwConnectionAlreadyClosedException()
                ConnectingState, is ConnectedState -> {
                    paused.set(true)
                    return
                }

                CreatedState -> throwConnectionNotStartedYetException()
                PausedState -> it
                WaitingState -> PausedState
            }
        }
    }

    override fun resume() {
        stateFlow.update {
            when (it) {
                ClosedState, ClosingState -> throwConnectionAlreadyClosedException()
                ConnectingState, is ConnectedState -> {
                    paused.set(true)
                    return
                }

                CreatedState -> throwConnectionNotStartedYetException()
                PausedState -> ConnectingState
                WaitingState -> error("The connection is not paused yet.")
            }
        }
        createConnectingJob()
    }

    override fun ensureResumed() {
        stateFlow.update {
            when (it) {
                ClosedState, ClosingState -> throwConnectionAlreadyClosedException()
                ConnectingState, is ConnectedState -> {
                    paused.set(true)
                    return
                }

                CreatedState -> throwConnectionNotStartedYetException()
                PausedState -> ConnectingState
                WaitingState -> return
            }
        }
        createConnectingJob()
    }

    override suspend fun connect() {
        while (!isClosingOrClosed) {
            if (isConnected) {
                return
            }
            await()
        }
        throwConnectionAlreadyClosedException()
    }

    override suspend fun ensureConnected() {
        ensureStarted()
        connect()
    }

    override suspend fun send(data: Frame) {
        session.send(data)
    }

    override suspend fun receive(data: Frame, origin: Any?) {
        (incoming as Channel<Received<Frame>>).send(
            Received(
                data,
                origin
            )
        )
    }

    override suspend fun await() {
        when (stateFlow.value) {
            ClosedState, ClosingState -> throwConnectionAlreadyClosedException()
            else -> stateFlow.collect()
        }
    }

    override suspend fun close(cause: Cause, subject: Subject) {
        stateFlow.update {
            when (it) {
                ClosedState, ClosingState -> throwConnectionAlreadyClosedException()
                is ConnectedState -> {
                    it.session.close()
                    ClosingState
                }

                else -> ClosingState
            }
        }

        job.cancelAndJoin()

        if (manageHttpClient) {
            httpClient.close()
        }
    }

    override suspend fun close() = close(Cause("Client closed."), this)
}