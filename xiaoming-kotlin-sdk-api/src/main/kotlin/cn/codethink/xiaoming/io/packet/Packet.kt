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

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.PACKET_TYPE_RECEIPT
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.currentTimeSeconds
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.setValue
import com.fasterxml.jackson.annotation.JsonTypeName

/**
 * Packet is used to be sent between different software uses xiaoming standard.
 *
 * @author Chuanwise
 * @see RequestPacket
 * @see ReceiptPacket
 */
abstract class Packet(
    raw: Raw
) : AbstractData(raw) {
    abstract val id: Id
    abstract val type: String
    abstract val time: Long
    abstract val cause: Cause?
    abstract val session: SubjectDescriptor?
}

/**
 * Request packet represents a request from a software to another software.
 *
 * @author Chuanwise
 */
@JsonTypeName(PACKET_TYPE_REQUEST)
class RequestPacket(
    raw: Raw
) : Packet(raw) {
    override var id: Id by raw
    override var type: String by raw
    override var time: Long by raw
    override var cause: Cause by raw
    override var session: SubjectDescriptor? by raw

    var action: String by raw
    var mode: String by raw
    var argument: Any? by raw
    var timeout: Long by raw

    @JvmOverloads
    constructor(
        id: Id,
        action: String,
        mode: String,
        timeout: Long,
        cause: Cause,
        argument: Any? = null,
        time: Long = currentTimeSeconds,
        session: SubjectDescriptor? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        this.id = id
        this.type = PACKET_TYPE_REQUEST
        this.time = time
        this.cause = cause
        this.session = session

        this.action = action
        this.mode = mode
        this.argument = argument
        this.timeout = timeout
    }
}

const val PACKET_TYPE_REQUEST = "request"

/**
 * Receipt packet represents a response of a request packet.
 *
 * @author Chuanwise
 */
@JsonTypeName(PACKET_TYPE_RECEIPT)
class ReceiptPacket(
    raw: Raw
) : Packet(raw) {
    override var id: Id by raw
    override var type: String by raw
    override var time: Long by raw
    override var cause: Cause? by raw
    override var session: SubjectDescriptor? by raw

    var target: Id by raw
    var state: String by raw
    var data: Any? by raw

    @JvmOverloads
    constructor(
        id: Id,
        target: Id,
        state: String,
        cause: Cause? = null,
        data: Any? = null,
        time: Long = currentTimeSeconds,
        session: SubjectDescriptor? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        this.id = id
        this.type = PACKET_TYPE_REQUEST
        this.time = time
        this.cause = cause
        this.session = session

        this.target = target
        this.state = state
        this.data = data
    }
}