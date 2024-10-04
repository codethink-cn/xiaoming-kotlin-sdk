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

package cn.codethink.xiaoming.io.action

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.CauseSubjectPair
import cn.codethink.xiaoming.common.REQUEST_PACKET_FIELD_ARGUMENT
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.io.connection.Connection
import cn.codethink.xiaoming.io.connection.PacketConnection
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.packet.ReceiptPacket
import cn.codethink.xiaoming.io.packet.RequestPacket

/**
 * The context of a request.
 *
 * @author Chuanwise
 */
interface RequestContext<P, R> {
    val action: Action<P, R>
    val mode: String
    val timeout: Long
    val argument: P?
    val subject: SubjectDescriptor?
    val subjectOrDefault: SubjectDescriptor
    val time: Long
    val cause: Cause?
    val raw: Raw
    val receipt: ReceiptPacket
    val connection: Connection<*>

    var disconnect: CauseSubjectPair?
}

data class PacketRequestContext<P, R>(
    override val action: Action<P, R>,
    val request: RequestPacket,
    private val defaultSubject: SubjectDescriptor,
    override val receipt: ReceiptPacket,
    override val connection: PacketConnection<*>,
    override var disconnect: CauseSubjectPair? = null
) : RequestContext<P, R> {
    override val mode: String by request::mode
    override val timeout: Long by request::timeout

    @Suppress("UNCHECKED_CAST")
    override val argument: P? = request.raw.get(
        name = REQUEST_PACKET_FIELD_ARGUMENT,
        type = action.requestPara.type,
        optional = action.requestPara.optional,
        nullable = action.requestPara.nullable
    ) as P?

    override val subject: SubjectDescriptor? by request::subject
    override val subjectOrDefault: SubjectDescriptor
        get() = subject ?: defaultSubject

    override val time: Long by request::time
    override val cause: Cause? by request::cause

    override val raw: Raw by request::raw
}