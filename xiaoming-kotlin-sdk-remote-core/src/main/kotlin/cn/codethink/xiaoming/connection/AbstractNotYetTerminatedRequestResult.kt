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

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.packet.ReceiptPacket
import cn.codethink.xiaoming.packet.RequestPacket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class AbstractNotYetTerminatedRequestResult<T>(
    override val request: RequestPacket,
    override val receipt: Received<ReceiptPacket>,
    override val session: Session,
    private val channel: Channel<TerminatedRequestResult<T>>
) : AbstractRequestResult<T>(request, session), NotYetTerminatedRequestResult<T> {
    private val lock = ReentrantReadWriteLock()
    private var result: TerminatedRequestResult<T>? = null

    override fun await(time: Long, unit: TimeUnit): TerminatedRequestResult<T>? {
        lock.read {
            if (result != null) {
                return result
            }
        }
        lock.write {
            if (result != null) {
                return result
            }
            runBlocking {
                withTimeoutOrNull(unit.toMillis(time)) {
                    channel.receive()
                }
            }?.let { result = it }
        }
        return result
    }

    override fun await(): TerminatedRequestResult<T> {
        lock.read {
            val result = result
            if (result != null) {
                return result
            }
        }
        lock.write {
            val result = result
            if (result != null) {
                return result
            }

            val received = runBlocking {
                channel.receive()
            }
            this.result = received
            return received
        }
    }
}

class NotYetTerminatedReceivedRequestResultImpl<T>(
    override val request: RequestPacket,
    override val receipt: Received<ReceiptPacket>,
    override val session: Session,
    channel: Channel<TerminatedRequestResult<T>>
) : AbstractNotYetTerminatedRequestResult<T>(request, receipt, session, channel)