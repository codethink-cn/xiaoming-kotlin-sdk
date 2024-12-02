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
import cn.codethink.xiaoming.util.Cause

abstract class AbstractTerminatedRequestResult<T>(
    override val request: RequestPacket,
    override val receipt: Received<ReceiptPacket>,
    override val session: Session,
) : AbstractRequestResult<T>(request, session), TerminatedRequestResult<T>

class SucceedRequestResultImpl<T>(
    override val request: RequestPacket,
    override val receipt: Received<ReceiptPacket>,
    override val session: Session,
    override val data: T
) : AbstractTerminatedRequestResult<T>(request, receipt, session), SucceedRequestResult<T>

class FailedRequestResultImpl<T>(
    override val request: RequestPacket,
    override val receipt: Received<ReceiptPacket>,
    override val session: Session,
    override val cause: Cause
) : AbstractTerminatedRequestResult<T>(request, receipt, session), FailedRequestResult<T>

class InterruptedRequestResultImpl<T>(
    override val request: RequestPacket,
    override val receipt: Received<ReceiptPacket>,
    override val session: Session,
    override val cause: Cause
) : AbstractTerminatedRequestResult<T>(request, receipt, session), InterruptedRequestResult<T>

class CancelledRequestResultImpl<T>(
    override val request: RequestPacket,
    override val receipt: Received<ReceiptPacket>,
    override val session: Session,
    override val cause: Cause
) : AbstractTerminatedRequestResult<T>(request, receipt, session), CancelledRequestResult<T>

class TerminatedReceivedRequestResultImpl<T>(
    override val request: RequestPacket,
    override val receipt: Received<ReceiptPacket>,
    override val session: Session
) : AbstractTerminatedRequestResult<T>(request, receipt, session), ReceivedRequestResult<T>