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
import cn.codethink.xiaoming.common.HEADER_VALUE_AUTHORIZATION_BEARER_WITH_SPACE
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.TextCause
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.EOFException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

val CONNECTION_SUBJECT_DESCRIPTOR_Descriptor_ATTRIBUTE_KEY = AttributeKey<SubjectDescriptor>("Connection Subject")

/**
 * WebSocket server for local platform.
 *
 * It uses [authorizationService] to authorize and allocate a connection subject to check
 * its permission.
 *
 * @author Chuanwise
 * @see WebSocketServerApi
 * @see AuthorizationService
 */
class LocalPlatformWebSocketServerApi(
    val configuration: WebSocketServerConfiguration,
    descriptor: SubjectDescriptor,
    val authorizationService: AuthorizationService,
    private val logger: KLogger = KotlinLogging.logger { },
    applicationEngineFactory: ApplicationEngineFactory<*, *> = Netty,
    parentJob: Job? = null,
    parentCoroutineContext: CoroutineContext = Dispatchers.IO
) : WebSocketServerApi(
    configuration, descriptor, logger, applicationEngineFactory, parentJob, parentCoroutineContext
) {
    private val mutableConnections = mutableListOf<OnlineConnectionInternalApi>()
    override val connectionApis: List<OnlineConnectionInternalApi>
        get() = lock.read { mutableConnections.toList() }

    enum class OnlineState {
        INITIALIZING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED,
        CLOSING,
        CLOSED,
    }

    inner class OnlineConnectionInternalApi(
        override val session: WebSocketServerSession,
        override val descriptor: SubjectDescriptor,
        parentJob: Job,
        parentCoroutineContext: CoroutineContext
    ) : WebSocketConnectionInternalApi {
        private val supervisorJob = SupervisorJob(parentJob)
        private val scope = CoroutineScope(parentCoroutineContext + supervisorJob)
        override val coroutineContext: CoroutineContext = scope.coroutineContext

        override val channel: Channel<Frame> = Channel(Channel.UNLIMITED)

        private val onlineLock = ReentrantReadWriteLock()
        private val onlineLockCondition = onlineLock.writeLock().newCondition()
        private var stateNoLock = OnlineState.INITIALIZING

        override val isConnected: Boolean
            get() = onlineLock.read { stateNoLock == OnlineState.CONNECTED }

        override val isConnecting: Boolean = false

        override val isClosed: Boolean
            get() = onlineLock.read { stateNoLock == OnlineState.CLOSED }

        private fun assertConnected() {
            if (!isConnected) {
                throw IllegalStateException("Connection is not connected.")
            }
        }

        override suspend fun send(data: Frame) {
            assertConnected()
            sendNoCheck(data)
        }

        private suspend fun sendNoCheck(frame: Frame) {
            val session = onlineLock.read { session }
            session.send(frame)
            session.flush()
            logger.debug { "Sent: '$frame' to ${configuration.address}." }
        }

        override suspend fun receive(data: Frame) {
            assertConnected()
            channel.send(data)
            logger.debug { "Received: '$data' from ${configuration.address}." }
        }

        internal suspend fun receiving() {
            authorizationService.onConnected(this)
            val address = "${session.call.request.host()}:${session.call.request.port()}${session.call.request.path()}"
            try {
                for (frame in session.incoming) {
                    receive(frame)
                }
            } catch (_: EOFException) {
            } catch (_: CancellationException) {
            } catch (exception: Exception) {
                logger.error(exception) { "Error occurred when receiving from client $address." }
            } finally {
                try {
                    lock.write { mutableConnections.remove(this) }
                    authorizationService.onDisconnected(this)
                } finally {
                    onlineLock.write {
                        stateNoLock = OnlineState.DISCONNECTED
                    }
                }
            }
        }

        override fun close() = close(TextCause("Server closed.", descriptor))

        override fun close(cause: Cause) = onlineLock.write {
            stateNoLock = when (stateNoLock) {
                OnlineState.INITIALIZING, OnlineState.CONNECTED, OnlineState.DISCONNECTING, OnlineState.DISCONNECTED -> OnlineState.CLOSING
                else -> throw IllegalStateException("Online connection internal error: unexpected state before close: $stateNoLock.")
            }
            runBlocking {
                session.close()
            }
            supervisorJob.cancel()
            stateNoLock = OnlineState.CLOSED

            logger.debug { "Online connection with subject: $descriptor caused by $cause." }
        }

        override fun await() = onlineLock.write {
            assertConnected()
            onlineLockCondition.await()
        }

        override fun await(time: Long, unit: TimeUnit): Boolean = onlineLock.write {
            assertConnected()
            onlineLockCondition.await(time, unit)
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

        val connectionSubject = authorizationService.authorize(token)
        if (connectionSubject == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        // 3. Record connection subject in call attributes.
        call.attributes.computeIfAbsent(CONNECTION_SUBJECT_DESCRIPTOR_Descriptor_ATTRIBUTE_KEY) { connectionSubject }
    }

    override suspend fun WebSocketServerSession.onConnected(
        parentJob: Job, parentCoroutineContext: CoroutineContext
    ) {
        // 1. Create and add connection.
        val connection = lock.write {
            val connectionSubject = call.attributes[CONNECTION_SUBJECT_DESCRIPTOR_Descriptor_ATTRIBUTE_KEY]
            OnlineConnectionInternalApi(this, connectionSubject, parentJob, parentCoroutineContext).apply {
                mutableConnections.add(this)
            }
        }

        // 2. Start receiving.
        connection.receiving()
    }
}
