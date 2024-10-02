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
import cn.codethink.xiaoming.io.packet.Packet
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KLogger
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext


/**
 * Represents a received data.
 *
 * @author Chuanwise
 */
interface Received<T> {
    /**
     * The origin of the data. Maybe an HTTP request call, a WebSocket frame,
     * or null for no origin data.
     */
    val origin: T?

    /**
     * The packet received.
     */
    val packet: Packet

    /**
     * The source of the data.
     */
    val source: ConnectionApi<T>
}

data class DefaultReceived<T>(
    override val origin: T?,
    override val packet: Packet,
    override val source: ConnectionApi<T>
) : Received<T>

/**
 * Represents a connection that can send and receive packets.
 *
 * @author Chuanwise
 */
interface ConnectionApi<T> : AutoCloseable, CoroutineScope {
    val isClosed: Boolean
    val isShared: Boolean

    operator fun get(subject: Subject) : Channel<Received<T>>

    suspend fun send(packet: Packet)
    suspend fun receive(packet: Packet, origin: T? = null)
    fun remove(subject: Subject) : Boolean
}

val ConnectionApi<*>.isNotShared: Boolean
    get() = !isShared

val ConnectionApi<*>.isNotClosed: Boolean
    get() = !isClosed

fun ConnectionApi<*>.assertNotClosed() {
    if (isClosed) {
        throw IllegalStateException("Connection has been closed.")
    }
}

/**
 * @author Chuanwise
 * @see ConnectionApi
 */
class TextFrameConnectionApi(
    private val logger: KLogger,
    private val objectMapper: ObjectMapper,
    private val connectionInternalApi: ConnectionInternalApi<Frame>,
) : ConnectionApi<Frame.Text> {
    override val coroutineContext: CoroutineContext by connectionInternalApi::coroutineContext

    private val lock = ReentrantReadWriteLock()
    private var closedNoLock: Boolean = false

    private val channels: MutableMap<Subject, Channel<Received<Frame.Text>>> = HashMap()
    private val receivingJob = launch {
        for (frame in connectionInternalApi.channel) {
            onReceiveFrameAsync(frame)
        }
        onBeClosed()
    }

    override val isClosed: Boolean
        get() = lock.read { closedNoLock }
    override val isShared: Boolean
        get() = lock.read { channels.size > 1 }

    override operator fun get(subject: Subject) : Channel<Received<Frame.Text>> {
        return channels.computeIfAbsent(subject) { Channel(Channel.UNLIMITED) }
    }

    override suspend fun send(packet: Packet) {
        assertNotClosed()

        val text = objectMapper.writeValueAsString(packet)
        val frame = Frame.Text(text)

        connectionInternalApi.send(frame)
    }

    override suspend fun receive(packet: Packet, origin: Frame.Text?) {
        assertNotClosed()

        val session = packet.subject
        val channel = getChannelOrNull(session) ?: return

        val received = DefaultReceived(origin, packet, this)
        channel.send(received)
    }

    private fun onReceiveFrameAsync(frame: Frame) = launch {
        // 1. Check if the frame is a text frame.
        if (frame !is Frame.Text) {
            logger.error { "Received a non-text frame: $frame, ignored." }
            return@launch
        }

        // 2. Parse the text to a packet.
        val text = frame.readText()
        logger.trace { "Received text: '$text'."}

        val packet = try {
            objectMapper.readValue<Packet>(text)
        } catch (exception: Exception) {
            logger.error(exception) { "Failed to parse packet from text: $text" }
            return@launch
        }

        // 3. Forward the packet to the channel.
        receive(packet, frame)
    }

    private fun getChannelOrNull(target: Subject?) : Channel<Received<Frame.Text>>? = lock.read {
        if (target == null) {
            val exclusiveChannel = channels.values.singleOrNull()
            if (exclusiveChannel == null) {
                if (channels.values.isNotEmpty()) {
                    logger.error { "Received a packet without target, " +
                            "but it's a shared connection with ${channels.values.size} channels." }
                } else {
                    logger.error { "Received a packet without target, but it's a connection without any channel." }
                }
                return@read null
            }
            exclusiveChannel
        } else {
            val sharedChannel = channels[target]
            if (sharedChannel == null) {
                logger.error { "Received a packet with target $target, but there's no channel for it." }
                return@read null
            }
            sharedChannel
        }
    }

    override fun remove(subject: Subject) = lock.write { channels.remove(subject) != null }

    override fun close() {
        onBeClosed()
        connectionInternalApi.close()
    }

    private fun onBeClosed(): Unit = lock.write {
        if (closedNoLock) {
            throw IllegalStateException("Connection has been closed.")
        }
        closedNoLock = true

        receivingJob.cancel()
        channels.values.forEach {
            it.close()
        }
    }
}
