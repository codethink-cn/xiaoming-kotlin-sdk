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

@file:OptIn(InternalApi::class, InternalApi::class)

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.action.Action
import cn.codethink.xiaoming.packet.BusinessPacket
import cn.codethink.xiaoming.packet.HeartbeatData
import cn.codethink.xiaoming.packet.META_PACKET_ACTION_HEARTBEAT
import cn.codethink.xiaoming.packet.META_PACKET_ACTION_START
import cn.codethink.xiaoming.packet.META_PACKET_ACTION_STARTED
import cn.codethink.xiaoming.packet.META_PACKET_ACTION_STOP
import cn.codethink.xiaoming.packet.META_PACKET_ACTION_STOPPED
import cn.codethink.xiaoming.packet.MetaPacket
import cn.codethink.xiaoming.packet.MetaPacketImpl
import cn.codethink.xiaoming.packet.Packet
import cn.codethink.xiaoming.util.FIELD_DATA
import cn.codethink.xiaoming.packet.RECEIPT_STATE_CANCELLED
import cn.codethink.xiaoming.packet.RECEIPT_STATE_FAILED
import cn.codethink.xiaoming.packet.RECEIPT_STATE_INTERRUPTED
import cn.codethink.xiaoming.packet.RECEIPT_STATE_RECEIVED
import cn.codethink.xiaoming.packet.RECEIPT_STATE_SUCCEED
import cn.codethink.xiaoming.packet.RECEIPT_STATE_UNDEFINED
import cn.codethink.xiaoming.packet.ReceiptPacket
import cn.codethink.xiaoming.packet.ReceiptPacketImpl
import cn.codethink.xiaoming.packet.RequestMode
import cn.codethink.xiaoming.packet.RequestPacket
import cn.codethink.xiaoming.packet.SessionDescriptor
import cn.codethink.xiaoming.packet.reversed
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.ConnectionDescriptor
import cn.codethink.xiaoming.util.Data
import cn.codethink.xiaoming.util.RegistrationImpl
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MutableMapRegistrationManagerImpl
import cn.codethink.xiaoming.util.Registration
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.createCause
import cn.codethink.xiaoming.util.createRandomUniversalUniqueId
import cn.codethink.xiaoming.util.createSessionExistedCause
import cn.codethink.xiaoming.util.createSessionHandlerErroredCause
import cn.codethink.xiaoming.util.createSessionInvalidCause
import cn.codethink.xiaoming.util.createSessionRejectedCause
import cn.codethink.xiaoming.util.createSessionRequiredCause
import cn.codethink.xiaoming.util.createSessionUnsupportedCause
import cn.codethink.xiaoming.util.createUnsupportedRequestModeCause
import cn.codethink.xiaoming.util.currentTimeMillis
import cn.codethink.xiaoming.util.get
import cn.codethink.xiaoming.util.getValue
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

private typealias StringPair = Pair<String, String>

interface ConnectionConfiguration<T> {
    val logger: KLogger
    val descriptor: ConnectionDescriptor
    val connectionApi: ConnectionApi<T>
    val sessionStartTimeout: Long
    val sessionStopTimeout: Long
    val heartbeatInterval: Long?
    val watchdog: HeartbeatWatchdog?
    val language: SessionLanguageConfiguration
}

abstract class AbstractConnection<T>(
    private val configuration: ConnectionConfiguration<T>
) : Connection {
    private val logger by configuration::logger
    override val descriptor: ConnectionDescriptor by configuration::descriptor
    private val language by configuration::language
    private val connectionApi by configuration::connectionApi

    private val lock = ReentrantReadWriteLock()
    private var closedNoLock: Boolean = false

    private enum class State {
        FORWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED,
        FORWARD_STARTING_ACCEPTED_BY_OTHER_SIDE,

        BACKWARD_STARTING_WAITING_FOR_CURRENT_SIDE_ACCEPTED,
        BACKWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED,

        STARTING_ERRORED,
        STARTED,
        STOPPING,
        STOPPING_ERRORED,
        STOPPED
    }

    private inner class SessionImpl(
        override val descriptor: SessionDescriptor,
        private var stateNoLock: State,
        val sessionHandler: Registration<SessionHandler<Any?, Any?>>
    ) : AbstractSession(this) {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(job + connectionApi.coroutineContext)
        override val coroutineContext: CoroutineContext by scope::coroutineContext

        override val reversedDescriptor = descriptor.reversed

        private val lock = ReentrantReadWriteLock()
        private val condition = lock.writeLock().newCondition()

        var state: State
            get() = lock.read { stateNoLock }
            set(value) = lock.write {
                stateNoLock = value
                condition.signalAll()
            }

        override val isStopped: Boolean get() = state == State.STOPPED

        val isStarted: Boolean get() = state == State.STARTED
        val isStarting: Boolean
            get() = when (state) {
                State.FORWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED,
                State.FORWARD_STARTING_ACCEPTED_BY_OTHER_SIDE,
                State.BACKWARD_STARTING_WAITING_FOR_CURRENT_SIDE_ACCEPTED,
                State.BACKWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED -> true

                else -> false
            }

        override val subject: SubjectDescriptor = descriptor.to
        private val channels = ConcurrentHashMap<Id, Channel<Received<ReceiptPacket>>>()

        private lateinit var requestHandler: SessionRequestHandler

        var otherSideRejectedCause: Cause? = null
        var otherSideRejectedSubject: SubjectDescriptor? = null

        var currentSideRejectedCause: Cause? = null
        var currentSideRejectedSubject: SubjectDescriptor? = null

        inner class SessionForwardStartContextImpl<T>(
            override val packet: Received<MetaPacket>,
            override val data: T
        ) : SessionForwardStartContext<T> {
            override val connection: Connection = this@AbstractConnection

            override val isOtherSideOperated: Boolean = true
            override val isOtherSideAccepted: Boolean = true
            override val isOtherSideRejected: Boolean = false

            override val isCurrentSideOperated: Boolean get() = state != State.FORWARD_STARTING_ACCEPTED_BY_OTHER_SIDE
            override val isCurrentSideAccepted: Boolean get() = state == State.STARTED
            override val isCurrentSideRejected: Boolean get() = state == State.STOPPED

            private fun assertNotOperated() = check(!isCurrentSideOperated) { "Current side has been operated." }

            override fun accept(handler: SessionRequestHandler, cause: Cause?, subject: SubjectDescriptor?): Session {
                assertNotOperated()

                requestHandler = handler
                state = State.STARTED

                logger.info { "Session from local ${descriptor.from} to remote ${descriptor.to} accepted by both sides." }
                return this@SessionImpl
            }

            override fun reject(cause: Cause, subject: SubjectDescriptor?) {
                assertNotOperated()

                state = State.STOPPED

                currentSideRejectedCause = cause
                currentSideRejectedSubject = subject ?: sessionHandler.subject

                logger.info { "Session from local ${descriptor.from} to remote ${descriptor.to} accepted by the other side, but rejected by the current side." }
            }
        }

        var forwardStartContext: SessionForwardStartContextImpl<Any?>? = null

        inner class SessionBackwardStartContextImpl<T>(
            override val connection: Connection,
            override val packet: Received<MetaPacket>,
            private val descriptor: SessionDescriptor,
            private val timeout: Long
        ) : SessionBackwardStartContext<T> {
            private val channel = Channel<Received<MetaPacket>>(Channel.CONFLATED)

            override val isCurrentSideOperated: Boolean get() = state != State.BACKWARD_STARTING_WAITING_FOR_CURRENT_SIDE_ACCEPTED
            override val isCurrentSideAccepted: Boolean get() = state == State.BACKWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED
            override val isCurrentSideRejected: Boolean get() = state == State.STOPPED

            var requestHandler: SessionRequestHandler? = null

            override val isOtherSideOperated: Boolean get() = state != State.BACKWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED
            override val isOtherSideAccepted: Boolean get() = state == State.STARTED
            override val isOtherSideRejected: Boolean get() = state == State.STOPPED

            private fun assertNotOperated() = check(!isCurrentSideOperated) { "Session has been operated." }

            override suspend fun accept(
                data: T, handler: SessionRequestHandler, cause: Cause?, subject: SubjectDescriptor?
            ): Session {
                assertNotOperated()

                val packet = MetaPacketImpl(
                    id = createRandomUniversalUniqueId(),
                    action = META_PACKET_ACTION_STARTED,
                    data = data,
                    subject = subject ?: sessionHandler.subject,
                    session = descriptor,
                    cause = cause
                )
                requestHandler = handler
                connection.send(packet)

                val received = try {
                    withTimeout(timeout) {
                        channel.receive()

                    }
                } finally {
                    channel.close()
                }

                val accepted = when (received.data.action) {
                    META_PACKET_ACTION_STARTED -> {
                        state = State.STARTED
                        true
                    }

                    META_PACKET_ACTION_STOPPED -> {
                        state = State.STOPPED
                        otherSideRejectedCause = received.data.cause
                        false
                    }

                    else -> error("Unexpected action: ${received.data.action}.")
                }

                if (!accepted) {
                    throw SessionForwardRejectedExceptionImpl(received.data.cause?.message)
                }
                return this@SessionImpl
            }

            override fun reject(cause: Cause, subject: SubjectDescriptor?) {
                assertNotOperated()

                val packet = MetaPacketImpl(
                    id = createRandomUniversalUniqueId(),
                    action = META_PACKET_ACTION_STOPPED,
                    data = null,
                    subject = subject ?: sessionHandler.subject,
                    session = descriptor,
                    cause = cause
                )
                runBlocking {
                    connection.send(packet)
                }

                currentSideRejectedCause = cause
                currentSideRejectedSubject = subject ?: sessionHandler.subject

                channel.close()
                state = State.STOPPED
            }

            @InternalApi
            suspend fun onReceive(received: Received<MetaPacket>) {
                channel.send(received)
            }
        }

        var backwardStartContext: SessionBackwardStartContextImpl<Any?>? = null

        private fun assertNotStopped() = require(!isStopped) { "Session has been stopped." }
        private fun assertStarted() = require(isStarted) { "Session has not been started." }

        fun await(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) = lock.write {
            if (stateNoLock != State.STOPPING) {
                condition.await(time, unit)
            }
        }

        override suspend fun <P, R> request(
            action: Action<P, R>,
            mode: RequestMode,
            argument: P?,
            timeout: Long,
            cause: Cause,
            subject: SubjectDescriptor,
            time: Long
        ): RequestResult<R> {
            assertStarted()

            var requestPacketId: Id
            do {
                requestPacketId = createRandomUniversalUniqueId()
            } while (channels.containsKey(requestPacketId))

            val requestPacket = createRequestPacketToSend(
                id = requestPacketId,
                action = action.id,
                mode = mode,
                argument = argument,
                timeout = timeout,
                subject = subject,
                cause = cause,
            )

            val channel = Channel<Received<ReceiptPacket>>(Channel.CONFLATED)
            channels[requestPacketId] = channel

            send(requestPacket)
            val received = withTimeout(timeout) {
                channel.receive()
            }

            return when (mode) {
                RequestMode.SYNC, RequestMode.ASYNC -> {
                    channel.close()
                    channels.remove(requestPacketId)

                    parseRequestResult(requestPacket, received, action, mode)
                }

                RequestMode.FUTURE -> {
                    val resultChannel = Channel<TerminatedRequestResult<R>>(Channel.CONFLATED)
                    launch {
                        val terminated = channel.receive()
                        val terminatedResult = parseRequestResult(requestPacket, terminated, action, mode)

                        check(terminatedResult is TerminatedRequestResult<R>) { "Result must be terminated result!" }

                        channel.close()
                        channels.remove(requestPacketId)

                        resultChannel.send(terminatedResult)
                        resultChannel.close()
                    }

                    parseRequestResult(requestPacket, received, action, mode, resultChannel).also {
                        check(it is NotYetTerminatedRequestResult<R>) { "Result must be not yet terminated result!" }
                    }
                }
            }
        }

        private fun <R> parseRequestResult(
            request: RequestPacket, received: Received<ReceiptPacket>,
            action: Action<*, R>, mode: RequestMode, channel: Channel<TerminatedRequestResult<R>>? = null
        ): RequestResult<R> {
            val receipt = received.data
            return when (receipt.state) {
                RECEIPT_STATE_SUCCEED -> {
                    receipt as Data

                    @Suppress("UNCHECKED_CAST")
                    val data: R = receipt.raw.get(
                        name = FIELD_DATA,
                        type = action.receiptDataType.type,
                        optional = action.receiptDataType.optional,
                        nullable = action.receiptDataType.nullable,
                        convertable = action.receiptDataType.convertable,
                        defaultValueFactory = action.receiptDataType.defaultValueFactory
                    ) as R

                    SucceedRequestResultImpl(request, received, this, data)
                }

                RECEIPT_STATE_FAILED -> {
                    val cause = receipt.cause
                    requireNotNull(cause) { "Receipt packet with state 'failed' must have a cause! " }
                    FailedRequestResultImpl<R>(request, received, this, cause)
                }

                RECEIPT_STATE_INTERRUPTED -> TerminatedReceivedRequestResultImpl<R>(request, received, this)
                RECEIPT_STATE_RECEIVED -> when (mode) {
                    RequestMode.SYNC, RequestMode.ASYNC -> error("Unexpected receipt state 'received' for request with mode '$mode'.")
                    RequestMode.FUTURE -> NotYetTerminatedReceivedRequestResultImpl(request, received, this, channel!!)
                }

                RECEIPT_STATE_CANCELLED -> when (mode) {
                    RequestMode.SYNC, RequestMode.ASYNC -> error("Unexpected receipt state 'cancelled' for request with mode '$mode'.")
                    RequestMode.FUTURE -> {
                        val cause = receipt.cause
                        requireNotNull(cause) { "Receipt packet with state 'cancelled' must have a cause! " }
                        CancelledRequestResultImpl(request, received, this, cause)
                    }
                }

                else -> error("Unexpected receipt state '${receipt.state}'.")
            }
        }

        @Suppress("UNCHECKED_CAST")
        suspend fun onReceiveBusinessPacket(received: Received<BusinessPacket>) {
            assertNotStopped()

            when (val packet = received.data) {
                is ReceiptPacket -> {
                    val channel = channels[packet.request]
                    if (channel == null) {
                        logger.error { "Received a receipt packet for request id ${packet.request}, but no corresponding request found." }
                    } else {
                        channel.send(received as Received<ReceiptPacket>)
                    }
                }

                is RequestPacket -> onReceiveRequestPacketNoCheck(received as Received<RequestPacket>)
            }
        }

        private suspend fun onReceiveRequestPacketNoCheck(received: Received<RequestPacket>) {
            val request = received.data
            val mode = try {
                RequestMode.fromLowerCaseString(request.mode)
            } catch (t: NoSuchElementException) {
                logger.error { "Received a request packet with invalid mode: ${request.mode}." }
                send(
                    createReceiptPacketToSend(
                        id = createRandomUniversalUniqueId(),
                        request = request.id,
                        state = RECEIPT_STATE_FAILED,
                        data = null,
                        cause = language.createUnsupportedRequestModeCause(request.mode, descriptor)
                    )
                )
                return
            }

            val receipt = createReceiptPacketToSend(
                id = createRandomUniversalUniqueId(),
                request = request.id,
                state = RECEIPT_STATE_UNDEFINED,
                data = null,
                cause = null
            )
            val context = SessionRequestContextImpl(received, receipt, this, mode)
            requestHandler.handle(context)
        }

        @InternalApi
        suspend fun onReceiveMetaPacket(received: Received<MetaPacket>) {
            assertNotStopped()

            val packet = received.data
            when (packet.action) {
                META_PACKET_ACTION_START -> when (state) {
                    // 只有对方主动建立连接时，自己才会收到 start 数据包。
                    State.BACKWARD_STARTING_WAITING_FOR_CURRENT_SIDE_ACCEPTED -> onBackwardStartNoCheck(received)
                    else -> error("Unexpected state: '$state' for meta packet action '${packet.action}'.")
                }

                META_PACKET_ACTION_STARTED -> when (state) {
                    // 己方主动建立连接，等待对方同意时收到了 started，意味着对方同意，需要由己方确认。
                    State.FORWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED -> {
                        state = State.FORWARD_STARTING_ACCEPTED_BY_OTHER_SIDE
                        onForwardStartNoCheck(received)
                    }
                    // 己方主动建立连接，对方同意，随后等待己方确认时收到 stopped，可能是己方超时或对方主动关闭。
                    State.FORWARD_STARTING_ACCEPTED_BY_OTHER_SIDE -> {
                        otherSideRejectedCause = packet.cause
                        otherSideRejectedSubject = packet.subject

                        state = State.STOPPED
                        logger.error { "Session from local ${descriptor.from} to remote ${descriptor.to} stopped by other side: '${packet.cause?.message}'." }
                    }
                    // 对方主动建立连接，等待己方同意时收到 stopped，可能是己方超时或对方主动关闭。
                    State.BACKWARD_STARTING_WAITING_FOR_CURRENT_SIDE_ACCEPTED -> {
                        otherSideRejectedCause = packet.cause
                        otherSideRejectedSubject = packet.subject

                        state = State.STOPPED
                        backwardStartContext!!.onReceive(received)
                    }
                    // 对方主动建立连接，等待对方确认时收到 stopped，表示对方拒绝。
                    State.BACKWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED -> {
                        otherSideRejectedCause = packet.cause
                        otherSideRejectedSubject = packet.subject

                        state = State.STOPPED
                        backwardStartContext!!.onReceive(received)
                    }
                    // 己方主动提出关闭，向对方发送 stop，随后收到 stopped，表示对方已经成功关闭。
                    State.STOPPING -> {
                        state = State.STOPPED
                    }

                    else -> error("Unexpected state: '$state' for meta packet action '${packet.action}'.")
                }

                META_PACKET_ACTION_STOPPED -> when (state) {
                    // 己方主动建立连接，等待对方同意时收到了 stopped，意味着对方拒绝。
                    State.FORWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED -> {
                        otherSideRejectedCause = packet.cause
                        otherSideRejectedSubject = packet.subject

                        state = State.STOPPED
                        logger.error { "Session from local ${descriptor.from} to remote ${descriptor.to} rejected by other side: '${packet.cause?.message}'." }
                    }
                    // 己方主动建立连接，对方同意，随后等待己方确认时收到 stopped，可能是己方超时或对方主动关闭。
                    State.FORWARD_STARTING_ACCEPTED_BY_OTHER_SIDE -> {
                        otherSideRejectedCause = packet.cause
                        otherSideRejectedSubject = packet.subject

                        state = State.STOPPED
                        logger.error { "Session from local ${descriptor.from} to remote ${descriptor.to} stopped by other side: '${packet.cause?.message}'." }
                    }
                    // 对方主动建立连接，等待己方同意时收到 stopped，可能是己方超时或对方主动关闭。
                    State.BACKWARD_STARTING_WAITING_FOR_CURRENT_SIDE_ACCEPTED -> {
                        otherSideRejectedCause = packet.cause
                        otherSideRejectedSubject = packet.subject

                        state = State.STOPPED
                        backwardStartContext!!.onReceive(received)
                    }
                    // 对方主动建立连接，等待对方确认时收到 stopped，表示对方拒绝。
                    State.BACKWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED -> {
                        otherSideRejectedCause = packet.cause
                        otherSideRejectedSubject = packet.subject

                        state = State.STOPPED
                        backwardStartContext!!.onReceive(received)
                    }
                    // 己方主动提出关闭，向对方发送 stop，随后收到 stopped，表示对方已经成功关闭。
                    State.STOPPING -> {
                        state = State.STOPPED
                    }

                    else -> error("Unexpected state: '$state' for meta packet action '${packet.action}'.")
                }

                META_PACKET_ACTION_STOP -> onBeClosed(received)

                else -> error("Unexpected action: ${packet.action}.")
            }
        }

        private fun onBackwardStartNoCheck(received: Received<MetaPacket>) {
            val context = SessionBackwardStartContextImpl<Any?>(
                connection = this@AbstractConnection,
                packet = received,
                descriptor = descriptor,
                timeout = configuration.sessionStartTimeout
            )
            this.backwardStartContext = context

            try {
                sessionHandler.value.onBackwardStart(context)

                if (!context.isCurrentSideOperated) {
                    val cause = language.createSessionRejectedCause(descriptor)
                    context.reject(cause, descriptor)
                }
            } catch (t: Throwable) {
                if (!context.isCurrentSideOperated) {
                    val cause = language.createSessionHandlerErroredCause(descriptor)
                    context.reject(cause, descriptor)
                }

                state = State.STARTING_ERRORED
                logger.error(t) { "Exception occurred on starting session from ${descriptor.from} to ${descriptor.to}, rejected automatically." }
                return
            }

            state = if (context.isCurrentSideAccepted) {
                State.BACKWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED
            } else {
                State.STOPPED
            }
        }

        private fun onForwardStartNoCheck(received: Received<MetaPacket>) {
            val packet = received.data
            packet as Data

            val data: Any? = packet.raw.get(FIELD_DATA, sessionHandler.value.forwardDataType)
            val context = SessionForwardStartContextImpl(received, data)
            this.forwardStartContext = context

            try {
                sessionHandler.value.onForwardStart(context)

                if (!context.isCurrentSideOperated) {
                    val cause = language.createSessionRejectedCause(descriptor)
                    context.reject(cause, descriptor)
                }
            } catch (t: Throwable) {
                if (!context.isCurrentSideOperated) {
                    val cause = language.createSessionHandlerErroredCause(descriptor)
                    context.reject(cause, descriptor)
                }

                state = State.STARTING_ERRORED
                logger.error(t) { "Exception occurred on starting session from ${descriptor.from} to ${descriptor.to}, rejected automatically." }
            }

            state = if (context.isCurrentSideAccepted) {
                State.STARTED
            } else {
                State.STOPPED
            }
        }

        suspend fun onBeClosed(packet: Received<MetaPacket>) {
            lock.write {
                stateNoLock = when (stateNoLock) {
                    State.FORWARD_STARTING_ACCEPTED_BY_OTHER_SIDE, State.FORWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED,
                    State.BACKWARD_STARTING_WAITING_FOR_CURRENT_SIDE_ACCEPTED, State.BACKWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED,
                    State.STARTED -> State.STOPPING

                    State.STOPPING -> error("Session is stopping.")
                    State.STOPPED -> error("Session has been stopped.")
                    State.STARTING_ERRORED, State.STOPPING_ERRORED -> error("Session is errored.")
                }
            }

            try {
                val context = SessionStopContextImpl(
                    session = this, packet = packet,
                    cause = packet.data.cause!!, subject = packet.data.subject!!
                )
                sessionHandler.value.onStop(context)
                send(
                    createMetaPacketToSend(
                        id = createRandomUniversalUniqueId(),
                        action = META_PACKET_ACTION_STOPPED,
                        data = null
                    )
                )
                state = State.STOPPED
            } catch (t: Throwable) {
                state = State.STOPPING_ERRORED
            }
        }

        override suspend fun close(cause: Cause, subject: SubjectDescriptor) {
            lock.write {
                stateNoLock = when (stateNoLock) {
                    State.FORWARD_STARTING_ACCEPTED_BY_OTHER_SIDE, State.FORWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED,
                    State.BACKWARD_STARTING_WAITING_FOR_CURRENT_SIDE_ACCEPTED, State.BACKWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED,
                    State.STARTED -> State.STOPPING

                    State.STOPPING -> error("Session is stopping.")
                    State.STOPPED -> error("Session has been stopped.")
                    State.STARTING_ERRORED, State.STOPPING_ERRORED -> error("Session is errored.")
                }
            }

            try {
                val context = SessionStopContextImpl(session = this, packet = null, cause = cause, subject = subject)
                sessionHandler.value.onStop(context)

                val stopPacket = createMetaPacketToSend(
                    id = createRandomUniversalUniqueId(),
                    action = META_PACKET_ACTION_STOP,
                    data = null,
                    cause = cause
                )
                withTimeout(configuration.sessionStopTimeout) {
                    send(stopPacket)
                    await(configuration.sessionStopTimeout)
                }
                state = State.STOPPED
            } catch (t: Throwable) {
                state = State.STOPPING_ERRORED
            }
        }

        override fun close() = runBlocking {
            close(createCause("Session closed.", descriptor), descriptor)
        }
    }

    private val mutableSessions: MutableMap<SessionDescriptor, SessionImpl> = ConcurrentHashMap()
    override val sessions: Collection<Session> get() = mutableSessions.values.toList()

    private val sessionHandlers = MutableMapRegistrationManagerImpl<StringPair, SessionHandler<Any?, Any?>>()
    private val receivingJob = launch {
        for (frame in connectionApi.channel) {
            onReceiveFrameAsync(frame)
        }
        onChannelClosed()
    }

    override val isClosed: Boolean get() = lock.read { closedNoLock }
    override val isShared: Boolean get() = !isClosed && sessions.size > 1

    private var nextFeedTimeMillis: AtomicLong? = null
    private var lastFeedTimeMillis: AtomicLong? = null
    private var watchdogJob: Job? = null

    init {
        val heartbeatInterval = configuration.heartbeatInterval
        val watchdog = configuration.watchdog

        require((heartbeatInterval == null) == (watchdog == null)) {
            "Both heartbeatInterval and watchdog must be null or not null at the same time."
        }
        if (heartbeatInterval != null && watchdog != null) {
            val nextFeedTimeMillis = AtomicLong(currentTimeMillis + heartbeatInterval)
            val lastFeedTimeMillis = AtomicLong(currentTimeMillis)

            this.nextFeedTimeMillis = nextFeedTimeMillis
            this.lastFeedTimeMillis = lastFeedTimeMillis

            watchdogJob = launch {
                while (!isClosed) {
                    // Delay to next feed time millis.
                    val delayingTimeMillis = currentTimeMillis
                    var nextFeedTimeMillisBeforeDelaying: Long
                    do {
                        nextFeedTimeMillisBeforeDelaying = nextFeedTimeMillis.get()
                        delay(nextFeedTimeMillisBeforeDelaying - currentTimeMillis)
                    } while (!isClosed && nextFeedTimeMillisBeforeDelaying != nextFeedTimeMillis.get())

                    // If no packet received during the delay, call watchdog.
                    if (delayingTimeMillis > lastFeedTimeMillis.get()) {
                        watchdog.onTimeout(this@AbstractConnection)
                    }
                }
            }
        }
    }

    private fun assertNotClosed() {
        check(isClosed) { "Connection has been closed." }
    }

    @Suppress("UNCHECKED_CAST")
    override fun handle(type: String, to: String, handler: SessionHandler<*, *>, subject: SubjectDescriptor) {
        val key = StringPair(type, to)
        sessionHandlers.register(key, handler as SessionHandler<Any?, Any?>, subject)
    }

    protected abstract fun Packet.toFrame(): T

    protected abstract fun T.toPacket(): Packet

    override suspend fun send(packet: Packet) {
        assertNotClosed()

        val frame = packet.toFrame()
        connectionApi.send(frame)
    }

    override suspend fun receive(packet: Packet, origin: Any?) {
        assertNotClosed()

        val received: Received<Packet> = createReceived(origin, packet)
        onReceivePacket(received)
    }

    override suspend fun start(data: Any?, session: SessionDescriptor, cause: Cause): Session {
        assertNotClosed()

        val reversed = session.reversed

        val key = StringPair(session.from.type, session.to.type)
        val sessionHandler = sessionHandlers[key] ?: error("No session handler for session: $session.")

        val newSession = SessionImpl(
            descriptor = reversed,
            sessionHandler = sessionHandler,
            stateNoLock = State.FORWARD_STARTING_WAITING_FOR_OTHER_SIDE_ACCEPTED
        )

        val oldSession = mutableSessions.putIfAbsent(reversed, newSession)
        if (oldSession != null) {
            logger.error { "Session from ${session.from} to ${session.to} has already been opened." }
            return oldSession
        }

        val deadlineTimeMillis = configuration.sessionStartTimeout

        // Send the start session packet.
        send(
            MetaPacketImpl(
                id = createRandomUniversalUniqueId(),
                action = META_PACKET_ACTION_START,
                data = data,
                session = session,
                cause = cause
            )
        )

        while (newSession.isStarting) {
            val remainingTimeMillis = deadlineTimeMillis - currentTimeMillis
            if (remainingTimeMillis <= 0) {
                mutableSessions.remove(reversed)
                error("Session from ${session.from} to ${session.to} start timeout.")
            }

            newSession.await(remainingTimeMillis)
        }

        if (newSession.isStarted) {
            return newSession
        } else if (newSession.otherSideRejectedCause != null) {
            throw SessionForwardRejectedExceptionImpl(newSession.otherSideRejectedCause?.message)
        } else if (newSession.currentSideRejectedCause != null) {
            throw SessionBackwardRejectedExceptionImpl(newSession.currentSideRejectedCause?.message)
        } else {
            error("Session is not started, and rejected causes is null.")
        }
    }

    private fun onReceiveFrameAsync(received: Received<T>) = launch {
        val data = received.data
        val packet = data.toPacket()

        try {
            receive(packet, received)
        } catch (t: Throwable) {
            logger.error(t) { "Exception occurred on receiving packet: $packet from $received." }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun onReceivePacket(received: Received<Packet>) {
        val packet = received.data
        val type = packet.type

        // Feed dog.
        val watchdog = configuration.watchdog
        if (watchdog != null) {
            lastFeedTimeMillis!!.set(currentTimeMillis)
            watchdog.onFeed(this, received)
        }

        when (packet) {
            is MetaPacket -> onReceiveMetaPacket(received as Received<MetaPacket>)
            is RequestPacket -> {
                val session = mutableSessions[packet.session]
                if (session == null) {
                    logger.error { "Received a request packet with invalid session descriptor: ${packet.session}, replied failed receipt." }
                    send(
                        ReceiptPacketImpl(
                            id = createRandomUniversalUniqueId(),
                            request = packet.id,
                            state = RECEIPT_STATE_FAILED,
                            data = null,
                            session = packet.session.reversed,
                            cause = language.createSessionInvalidCause(packet.session, descriptor)
                        )
                    )
                    return
                }

                session.onReceiveBusinessPacket(received as Received<BusinessPacket>)
            }

            is ReceiptPacket -> {
                val session = mutableSessions[packet.session]
                if (session == null) {
                    logger.error { "Received a receipt packet with invalid session descriptor: ${packet.session}, ignored." }
                    return
                }

                session.onReceiveBusinessPacket(received as Received<BusinessPacket>)
            }

            else -> error("Unexpected packet type: $type.")
        }
    }

    private suspend fun onReceiveMetaPacket(received: Received<MetaPacket>) {
        val packet = received.data
        val watchdog = configuration.watchdog

        val session = packet.session
        if (session == null) {
            // Only heartbeat meta packet can have no session descriptor.
            if (packet.action != META_PACKET_ACTION_HEARTBEAT) {
                logger.error { "Received a meta packet without session descriptor: $packet and it is no heartbeat meta packet." }
                send(
                    MetaPacketImpl(
                        id = createRandomUniversalUniqueId(),
                        action = META_PACKET_ACTION_STOPPED,
                        data = null,
                        session = null,
                        cause = language.createSessionRequiredCause(descriptor)
                    )
                )
                return
            }
            packet as Data

            val data: HeartbeatData by packet.raw
            if (watchdog == null) {
                logger.debug { "Received a heartbeat packet: $packet, but watchdog disabled, ignored." }
            } else {
                nextFeedTimeMillis!!.set(data.interval + currentTimeMillis)
            }
            return
        }

        val target: SessionImpl
        if (packet.action == META_PACKET_ACTION_START) {
            val reversed = session.reversed

            // Check if session handler present.
            val key = StringPair(session.from.type, session.to.type)
            val registration = sessionHandlers[key]

            if (registration == null) {
                logger.warn { "Received a session start packet from ${session.from} to ${session.to}, but no handler present." }
                send(
                    MetaPacketImpl(
                        id = createRandomUniversalUniqueId(),
                        action = META_PACKET_ACTION_STOPPED,
                        data = null,
                        session = reversed,
                        cause = language.createSessionUnsupportedCause(session, descriptor)
                    )
                )
                return
            }

            // Check if session already opened.
            val newSession = SessionImpl(
                session, State.BACKWARD_STARTING_WAITING_FOR_CURRENT_SIDE_ACCEPTED, registration
            )
            val oldSession = mutableSessions.putIfAbsent(session, newSession)
            if (oldSession !== null) {
                logger.warn { "Received a session start packet from ${session.from} to ${session.to}, but the session has already been opened." }
                send(
                    MetaPacketImpl(
                        id = createRandomUniversalUniqueId(),
                        action = META_PACKET_ACTION_STOPPED,
                        data = null,
                        session = reversed,
                        cause = language.createSessionExistedCause(session, descriptor)
                    )
                )
                return
            }

            target = newSession
        } else {
            val oldSession = mutableSessions[session]
            if (oldSession == null) {
                logger.error { "Received a meta packet with invalid session descriptor: $session, ignored." }
                return
            }

            target = oldSession
        }

        target.onReceiveMetaPacket(received)
    }

    private fun onClose(cause: Cause, subject: SubjectDescriptor) = lock.write {
        if (closedNoLock) {
            return@write
        }
        closedNoLock = true

        mutableSessions.values.forEach {
            if (it.isStarted) {
                runBlocking {
                    it.close(cause, subject)
                }
            }
        }
        mutableSessions.clear()
        sessionHandlers.clear()
    }

    override suspend fun close(cause: Cause, subject: SubjectDescriptor) {
        onClose(cause, subject)
        connectionApi.close(cause, subject)
    }

    override fun close() = runBlocking {
        close(createCause("Connection closed.", descriptor), descriptor)
    }

    private fun onChannelClosed(): Unit = lock.write {
        onClose(createCause("Channel closed.", descriptor), descriptor)
    }
}
