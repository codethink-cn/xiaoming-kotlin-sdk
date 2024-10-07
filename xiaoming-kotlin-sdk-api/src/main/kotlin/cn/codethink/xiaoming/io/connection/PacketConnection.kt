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
import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.DefaultStringMapRegistrations
import cn.codethink.xiaoming.common.PACKET_TYPE_RECEIPT
import cn.codethink.xiaoming.common.RECEIPT_PACKET_FIELD_DATA
import cn.codethink.xiaoming.common.RECEIPT_STATE_SUCCEED
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.io.ProtocolLanguageConfiguration
import cn.codethink.xiaoming.io.action.Action
import cn.codethink.xiaoming.io.packet.PACKET_TYPE_REQUEST
import cn.codethink.xiaoming.io.packet.Packet
import cn.codethink.xiaoming.io.packet.PacketContext
import cn.codethink.xiaoming.io.packet.PacketHandler
import cn.codethink.xiaoming.io.packet.ReceiptPacket
import cn.codethink.xiaoming.io.packet.ReceiptPacketHandler
import cn.codethink.xiaoming.io.packet.RequestPacket
import cn.codethink.xiaoming.io.packet.RequestPacketHandler
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds


/**
 * Use the [connectionApi] to do requests and get their receipts by sending and
 * receiving [Packet].
 *
 * If [connectionApi] disconnected, it will cache the requests for a while and
 *
 * @param T origin type of the data.
 * @author Chuanwise
 */
class PacketConnection<T>(
    private val logger: KLogger,
    override val session: SubjectDescriptor,
    private val language: ProtocolLanguageConfiguration,
    override val descriptor: SubjectDescriptor,
    private val connectionApi: ConnectionApi<T>,
    parentJob: Job? = null,
    parentCoroutineContext: CoroutineContext = Dispatchers.IO,
) : Connection<T> {
    // Coroutines related fields.
    private val supervisorJob = SupervisorJob(parentJob)
    private val scope = CoroutineScope(supervisorJob + parentCoroutineContext)
    override val coroutineContext: CoroutineContext by scope::coroutineContext

    private val lock = ReentrantReadWriteLock()

    private var closedNoLock: Boolean = false
    override val isClosed: Boolean
        get() = lock.read { closedNoLock }

    // Packet receiving channel.
    private val channel = connectionApi[descriptor]
    private val receivingJob = launch {
        for (received in channel) {
            onReceiveAsync(received)
        }
        if (isNotClosed) {
            close(TextCause("Channel closed.", descriptor))
        }
    }

    private val types = DefaultStringMapRegistrations<PacketHandler>()

    @Suppress("UNCHECKED_CAST")
    private val receiptPacketHandler: ReceiptPacketHandler<T>
        get() = types[PACKET_TYPE_RECEIPT]?.value as ReceiptPacketHandler<T>?
            ?: throw IllegalStateException("No receipt packet handler.")

    init {
        registerTypeHandler(PACKET_TYPE_REQUEST, RequestPacketHandler(language, descriptor), descriptor)
        registerTypeHandler(PACKET_TYPE_REQUEST, ReceiptPacketHandler<T>(), descriptor)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <P, R> request(
        action: Action<P, R>,
        mode: String,
        timeout: Long,
        cause: Cause,
        argument: P?,
        time: Long
    ): Pair<Received<T>, R?> {
        val (id, channel) = receiptPacketHandler.allocate()
        val packet = RequestPacket(
            id = id,
            action = action.name,
            mode = mode,
            timeout = timeout,
            cause = cause,
            argument = argument,
            time = time,
            session = session
        )

        connectionApi.send(packet)

        val received = withTimeout(time.milliseconds) { channel.receive() }
        val receipt = received.packet as ReceiptPacket
        val data: R? = when (receipt.state) {
            RECEIPT_STATE_SUCCEED -> receipt.raw.get(
                name = RECEIPT_PACKET_FIELD_DATA,
                type = action.receiptData.type,
                nullable = action.receiptData.nullable,
                optional = action.receiptData.optional
            ) as R?
            else -> null
        }

        return received to data
    }

    private suspend fun onReceive(received: Received<T>) {
        val packet = received.packet
        val type = packet.type

        val registration = types[type]
        if (registration == null) {
            logger.warn { "No handler for packet type: '$type'." }
            return
        }

        val context = PacketContext(logger, this, connectionApi, received, language)
        try {
            registration.value.handle(context)
        } catch (e: Exception) {
            logger.error(e) { "Exception occurred during handling packet: ${received.packet} from ${received.source}, origin is ${received.origin}." }
            return
        }

        context.disconnect?.let { close(it) }
    }

    private fun onReceiveAsync(received: Received<T>) = launch {
        onReceive(received)
    }

    fun registerTypeHandler(type: String, handler: PacketHandler, subject: SubjectDescriptor) {
        types[type] = DefaultRegistration(handler, subject)
    }

    override fun close(cause: Cause): Unit = lock.write {
        if (closedNoLock) {
            throw IllegalStateException("Connection has been closed.")
        }
        closedNoLock = true

        // 1. Remove channel from connection API.
        if (!connectionApi.remove(cause.subject)) {
            throw IllegalStateException("Failed to remove the subject from the connection.")
        }

        // 2. Cancel channel.
        channel.cancel()

        // 3. Cancel receiving job.
        receivingJob.cancel()
    }

    override fun close() {
        close(TextCause("Connection closed.", descriptor))
    }
}