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

package cn.codethink.xiaoming.io.packet

import cn.codethink.xiaoming.io.connection.Received
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

/**
 * Receipt packet handler.
 *
 * @author Chuanwise
 */
class ReceiptPacketHandler<T> : PacketHandler {
    private val lock = ReentrantReadWriteLock()
    private val channels: MutableMap<String, Channel<Received<T>>> = HashMap()

    @Suppress("UNCHECKED_CAST")
    override suspend fun handle(context: PacketContext) {
        val receipt = context.received.packet as ReceiptPacket
        val target = receipt.target

        val channel = channels.remove(target)
        if (channel == null) {
            context.logger.warn { "No receipt channel found for request: $target." }
            return
        }

        channel.send(context.received as Received<T>)
    }

    fun allocate(
        factory: () -> Channel<Received<T>> = { Channel(Channel.UNLIMITED) }
    ) : Pair<String, Channel<Received<T>>> = lock.write {
        var id: String
        do {
            id = randomUuidString()
        } while (channels.containsKey(id))

        val channel = factory()
        channels[id] = channel

        return@write id to channel
    }
}