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

import cn.codethink.xiaoming.packet.RECEIPT_STATE_FAILED
import cn.codethink.xiaoming.packet.RECEIPT_STATE_INTERRUPTED
import cn.codethink.xiaoming.packet.RECEIPT_STATE_RECEIVED
import cn.codethink.xiaoming.packet.RECEIPT_STATE_SUCCEED
import cn.codethink.xiaoming.packet.ReceiptPacket
import cn.codethink.xiaoming.packet.RequestMode
import cn.codethink.xiaoming.packet.RequestPacket
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.createRandomUniversalUniqueId
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SessionRequestContextImpl(
    override val request: Received<RequestPacket>,
    override val receipt: ReceiptPacket,
    override val session: AbstractSession,
    override val mode: RequestMode
) : SessionRequestContext {
    private val lock = ReentrantReadWriteLock()

    private var operatedNoLock: Boolean = false
    private var receivedNoLock: Boolean = false

    override val isReceived: Boolean get() = lock.read { receivedNoLock }
    override val isOperated: Boolean get() = lock.read { operatedNoLock }
    override val isSucceed: Boolean get() = lock.read { operatedNoLock && receipt.state == RECEIPT_STATE_SUCCEED }
    override val isFailed: Boolean get() = lock.read { operatedNoLock && receipt.state == RECEIPT_STATE_FAILED }
    override val isInterrupted: Boolean get() = lock.read { operatedNoLock && receipt.state == RECEIPT_STATE_INTERRUPTED }

    private fun assertNotOperated() = check(!isOperated) { "Request has been operated" }
    private fun assertNotReceived() = check(!isReceived) { "Request has been received." }
    private fun assertReceived() = check(isReceived) { "Request has not been received yet." }

    override suspend fun received() = lock.write {
        assertNotOperated()
        assertNotReceived()

        when (mode) {
            RequestMode.SYNC -> Unit
            RequestMode.ASYNC, RequestMode.FUTURE -> session.connection.send(
                session.createReceiptPacketToSend(
                    id = createRandomUniversalUniqueId(),
                    request = request.data.id,
                    state = RECEIPT_STATE_RECEIVED,
                    data = null
                )
            )
        }
    }

    private suspend fun sendTerminatedReceiptIfNeeded() {
        when (mode) {
            RequestMode.ASYNC -> Unit
            RequestMode.SYNC, RequestMode.FUTURE -> session.connection.send(receipt)
        }
    }

    override suspend fun succeed(data: Any?) = lock.write {
        assertNotOperated()
        assertReceived()

        receipt.state = RECEIPT_STATE_SUCCEED
        receipt.data = data
        receipt.cause = null

        operatedNoLock = true
        sendTerminatedReceiptIfNeeded()
    }

    override suspend fun failed(cause: Cause) = lock.write {
        assertNotOperated()
        assertReceived()

        receipt.state = RECEIPT_STATE_FAILED
        receipt.data = null
        receipt.cause = cause

        operatedNoLock = true
        sendTerminatedReceiptIfNeeded()
    }

    override suspend fun interrupt(cause: Cause) = lock.write {
        assertNotOperated()
        assertReceived()

        receipt.state = RECEIPT_STATE_INTERRUPTED
        receipt.data = null
        receipt.cause = cause

        operatedNoLock = true
        sendTerminatedReceiptIfNeeded()
    }
}