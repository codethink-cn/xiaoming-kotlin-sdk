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
import cn.codethink.xiaoming.common.ErrorMessageCause
import cn.codethink.xiaoming.common.HEADER_VALUE_AUTHORIZATION_BEARER_WITH_SPACE
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.common.currentTimeMillis
import cn.codethink.xiaoming.connection.buildAdapterNotFoundArguments
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.io.ERROR_ADAPTER_NOT_FOUND
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.netty.Netty
import io.ktor.server.request.authorization
import io.ktor.server.request.host
import io.ktor.server.request.path
import io.ktor.server.request.port
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.EOFException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

val CONNECTION_SUBJECT_ATTRIBUTE_KEY = AttributeKey<Subject>("Connection Subject")

/**
 * WebSocket server for local platform.
 *
 * It uses [authorizer] to authorize and allocate a connection subject to check
 * its permission.
 *
 * @author Chuanwise
 * @see WebSocketServer
 * @see Authorizer
 */
class LocalPlatformWebSocketServer(
    val configuration: WebSocketServerConfiguration,
    subject: Subject,
    val authorizer: Authorizer,
    val internalApi: LocalPlatformInternalApi,
    applicationEngineFactory: ApplicationEngineFactory<*, *> = Netty,
    parentJob: Job? = null,
    parentCoroutineContext: CoroutineContext = Dispatchers.IO
) : WebSocketServer(
    configuration, internalApi.logger, subject, applicationEngineFactory, parentJob, parentCoroutineContext
) {
    private val mutableConnections = mutableListOf<OnlineConnection>()
    override val connections: List<OnlineConnection>
        get() = lock.read { mutableConnections.toList() }

    private val logger by internalApi::logger
    enum class OnlineState {
        INITIALIZING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED,
        CLOSING,
        CLOSED,
    }

    inner class OnlineConnection(
        val session: WebSocketServerSession,
        val connectionSubject: Subject,
        parentJob: Job,
        parentCoroutineContext: CoroutineContext
    ) : FrameConnection {
        private val supervisorJob = SupervisorJob(parentJob)
        private val scope = CoroutineScope(parentCoroutineContext + supervisorJob)
        override val coroutineContext: CoroutineContext = scope.coroutineContext

        override val channel: Channel<String> = Channel(Channel.UNLIMITED)

        private val onlineLock = ReentrantReadWriteLock()
        private var stateNoLock = OnlineState.INITIALIZING

        private var subjectNoLock: Subject? = null
        override var subject: Subject
            get() = onlineLock.read { subjectNoLock!! }
            set(value) = onlineLock.write { subjectNoLock = value }

        override val isConnected: Boolean
            get() = onlineLock.read { stateNoLock == OnlineState.CONNECTED }

        override val isClosed: Boolean
            get() = onlineLock.read { stateNoLock == OnlineState.CLOSED }

        private fun assertConnected() {
            if (!isConnected) {
                throw IllegalStateException("Connection is not connected.")
            }
        }

        override suspend fun send(string: String) {
            assertConnected()
            sendNoCheck(string)
        }

        private suspend fun sendNoCheck(string: String) {
            val session = onlineLock.read { session }
            session.send(string)
            session.flush()
            logger.debug { "Sent: '$string' to ${configuration.address}." }
        }

        override suspend fun receive(string: String) {
            assertConnected()
            channel.send(string)
            logger.debug { "Received: '$string' from ${configuration.address}." }
        }

        internal suspend fun receiving() {
            authorizer.onConnected(connectionSubject)
            val address = "${session.call.request.host()}:${session.call.request.port()}${session.call.request.path()}"
            try {
                sendNoCheck("Timestamp: $currentTimeMillis")
                for (frame in session.incoming) {
                    if (frame !is Frame.Text) {
                        logger.warn { "Unexpected incoming frame: $frame from $address, except `Frame.Text`!" }
                        continue
                    }

                    val text = frame.readText()
                    receive(text)
                }
            } catch (_: EOFException) {
            } catch (_: CancellationException) {
            } catch (exception: Exception) {
                logger.error(exception) { "Error occurred when receiving from client $address." }
            } finally {
                try {
                    lock.write { mutableConnections.remove(this) }
                    authorizer.onDisconnected(connectionSubject)
                } finally {
                    onlineLock.write {
                        stateNoLock = OnlineState.DISCONNECTED
                    }
                }
            }
        }

        override fun close() {
            close(TextCause("Server closed."), connectionSubject)
        }

        override fun close(cause: Cause, subject: Subject) = onlineLock.write {
            stateNoLock = when (stateNoLock) {
                OnlineState.INITIALIZING, OnlineState.CONNECTED, OnlineState.DISCONNECTING, OnlineState.DISCONNECTED -> OnlineState.CLOSING
                else -> throw IllegalStateException("Online connection internal error: unexpected state before close: $stateNoLock.")
            }
            runBlocking {
                session.close()
            }
            supervisorJob.cancel()
            stateNoLock = OnlineState.CLOSED

            logger.debug { "Online connection with subject: $connectionSubject closed by $subject caused by $cause." }
        }
    }

    override suspend fun PipelineContext<Unit, ApplicationCall>.onConnect() {
        // 1. Check "authorization" header and format.
        val authorizationHeader = call.request.authorization()
        if (authorizationHeader == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        if (!authorizationHeader.startsWith(HEADER_VALUE_AUTHORIZATION_BEARER_WITH_SPACE)) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        // 2. Get token and authorize.
        val token = authorizationHeader.substring(HEADER_VALUE_AUTHORIZATION_BEARER_WITH_SPACE.length)

        val connectionSubject = authorizer.authorize(token)
        if (connectionSubject == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        // 3. Record connection subject in call attributes.
        call.attributes.computeIfAbsent(CONNECTION_SUBJECT_ATTRIBUTE_KEY) { connectionSubject }
    }

    override suspend fun WebSocketServerSession.onConnected(
        parentJob: Job, parentCoroutineContext: CoroutineContext
    ) {
        // 1. Create and add connection.
        val connection = lock.write {
            val connectionSubject = call.attributes[CONNECTION_SUBJECT_ATTRIBUTE_KEY]
            OnlineConnection(this, connectionSubject, parentJob, parentCoroutineContext).apply {
                mutableConnections.add(this)
            }
        }

        // 2. Adapt connection.
        val adapter = internalApi.connectionManagerApi.getConnectionAdapter(subject.type)
        if (adapter == null) {
            val arguments = buildAdapterNotFoundArguments(subject.type)
            connection.close(
                ErrorMessageCause(
                    error = ERROR_ADAPTER_NOT_FOUND,
                    message = internalApi.languageConfiguration.connection.adapterNotFound.format(arguments),
                    context = arguments
                ), subject
            )
            return
        }
        adapter.adapt(internalApi, subject, connection)

        // 3. Start receiving.
        connection.receiving()
    }
}
