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
import cn.codethink.xiaoming.common.Subject
import io.github.oshai.kotlinlogging.KLogger
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

interface WebSocketConnectionsConfiguration {
    val port: Int
    val host: String?
    val path: String
}

val WebSocketConnectionsConfiguration.address: String
    get() = "$host:$port$path"

/**
 * Connections on WebSocket.
 *
 * @author Chuanwise
 */
abstract class WebSocketConnections(
    private val configuration: WebSocketConnectionsConfiguration,
    private val logger: KLogger,
    override val subject: Subject,
    applicationEngineFactory: ApplicationEngineFactory<*, *> = Netty,
    parentJob: Job? = null,
    parentCoroutineContext: CoroutineContext = Dispatchers.IO,
) : Connections {
    private val supervisorJob = SupervisorJob(parentJob)
    private val scope: CoroutineScope = CoroutineScope(parentCoroutineContext + supervisorJob)
    final override val coroutineContext: CoroutineContext by scope::coroutineContext

    protected val lock = ReentrantReadWriteLock()

    enum class State {
        INITIALIZED,
        STARTED,
        CLOSING,
        CLOSED,
    }

    private var stateNoLock = State.INITIALIZED
    private val state: State
        get() = lock.read { stateNoLock }

    override val isClosed: Boolean
        get() = state == State.CLOSED

    override val isStarted: Boolean
        get() = state == State.STARTED

    private val server = embeddedServer(
        applicationEngineFactory,
        parentCoroutineContext = coroutineContext,
        port = configuration.port,
        host = configuration.host ?: "0.0.0.0"
    ) {
        module()
    }.start()

    private fun Application.module() {
        lock.write {
            stateNoLock = when (stateNoLock) {
                State.INITIALIZED -> State.STARTED
                State.CLOSING -> return
                else -> throw IllegalStateException(
                    "Server internal error: unexpected state after started: $stateNoLock."
                )
            }
        }

        install(WebSockets)
        intercept(ApplicationCallPipeline.Setup) {
            onConnect()
        }
        routing {
            webSocket(configuration.path) {
                onConnected(supervisorJob, coroutineContext)
            }
        }
    }

    override fun close(cause: Cause, subject: Subject) {
        lock.write {
            stateNoLock = when (stateNoLock) {
                State.INITIALIZED, State.STARTED -> State.CLOSING
                else -> throw IllegalStateException("Client internal error: unexpected state before closing: $stateNoLock.")
            }
        }

        connections.forEach {
            it.close(cause, subject)
        }

        server.stop()
        supervisorJob.cancel()

        lock.write {
            stateNoLock = when (stateNoLock) {
                State.CLOSING -> State.CLOSED
                else -> throw IllegalStateException("Client internal error: unexpected state after closing: $stateNoLock.")
            }
        }
        logger.debug { "Closed server with subject: $subject in ${configuration.address}." }
    }

    abstract suspend fun PipelineContext<Unit, ApplicationCall>.onConnect()

    abstract suspend fun WebSocketServerSession.onConnected(parentJob: Job, parentCoroutineContext: CoroutineContext)
}
