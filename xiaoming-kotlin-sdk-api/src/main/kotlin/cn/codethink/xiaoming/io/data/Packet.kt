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

package cn.codethink.xiaoming.io.data

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.PACKET_TYPE_RECEIPT
import cn.codethink.xiaoming.common.PACKET_TYPE_REQUEST
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.currentTimeSeconds
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
    var id: String by raw
    var type: String by raw
    var time: Long by raw
    var cause: Cause? by raw

    @JvmOverloads
    constructor(
        id: String,
        type: String,
        time: Long = currentTimeSeconds,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        this.id = id
        this.type = type
        this.time = time
        this.cause = cause
    }
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
    var action: String by raw
    var mode: String by raw
    var argument: Any? by raw
    var subject: Subject? by raw
    var timeout: Long by raw

    @JvmOverloads
    constructor(
        id: String,
        action: String,
        mode: String,
        timeout: Long,
        argument: Any? = null,
        subject: Subject? = null,
        time: Long = currentTimeSeconds,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        this.id = id
        this.type = PACKET_TYPE_REQUEST
        this.time = time
        this.cause = cause
        this.action = action
        this.mode = mode
        this.argument = argument
        this.subject = subject
        this.timeout = timeout
    }
}

/**
 * Receipt packet represents a response of a request packet.
 *
 * @author Chuanwise
 */
@JsonTypeName(PACKET_TYPE_RECEIPT)
class ReceiptPacket(
    raw: Raw
) : Packet(raw) {
    var state: String by raw
    var request: String by raw
    var data: Any? by raw

    @JvmOverloads
    constructor(
        id: String,
        state: String,
        request: String,
        data: Any? = null,
        time: Long = currentTimeSeconds,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        this.id = id
        this.type = PACKET_TYPE_REQUEST
        this.time = time
        this.cause = cause
        this.state = state
        this.request = request
        this.data = data
    }
}