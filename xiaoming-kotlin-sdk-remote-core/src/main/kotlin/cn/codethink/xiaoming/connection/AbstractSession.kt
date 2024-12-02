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

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.packet.MetaPacket
import cn.codethink.xiaoming.packet.MetaPacketImpl
import cn.codethink.xiaoming.packet.ReceiptPacket
import cn.codethink.xiaoming.packet.ReceiptPacketImpl
import cn.codethink.xiaoming.packet.RequestMode
import cn.codethink.xiaoming.packet.RequestPacket
import cn.codethink.xiaoming.packet.RequestPacketImpl
import cn.codethink.xiaoming.packet.SessionDescriptor
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.SubjectDescriptor

abstract class AbstractSession(
    val connection: AbstractConnection<*>
) : Session {
    protected abstract val reversedDescriptor: SessionDescriptor

    fun createRequestPacketToSend(
        id: Id,
        action: Id,
        mode: RequestMode,
        argument: Any?,
        timeout: Long,
        subject: SubjectDescriptor,
        cause: Cause
    ): RequestPacket = RequestPacketImpl(
        id = id,
        action = action,
        mode = mode.toLowerCaseString(),
        argument = argument,
        timeout = timeout,
        subject = subject,
        session = reversedDescriptor,
        cause = cause
    )

    fun createReceiptPacketToSend(
        id: Id,
        request: Id,
        state: String,
        data: Any?,
        cause: Cause? = null
    ): ReceiptPacket = ReceiptPacketImpl(
        id = id,
        request = request,
        state = state,
        data = data,
        session = reversedDescriptor,
        cause = cause
    )

    fun createMetaPacketToSend(
        id: Id,
        action: String,
        data: Any?,
        subject: SubjectDescriptor? = null,
        cause: Cause? = null
    ): MetaPacket = MetaPacketImpl(
        id = id,
        action = action,
        data = data,
        subject = subject,
        session = reversedDescriptor,
        cause = cause
    )
}