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
import cn.codethink.xiaoming.common.CAUSE_FIELD_CAUSE
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.PACKET_FIELD_ID
import cn.codethink.xiaoming.common.PACKET_FIELD_TIME
import cn.codethink.xiaoming.common.PACKET_FIELD_TYPE
import cn.codethink.xiaoming.common.PACKET_TYPE_RECEIPT
import cn.codethink.xiaoming.common.PACKET_TYPE_REQUEST
import cn.codethink.xiaoming.common.RECEIPT_PACKET_FIELD_DATA
import cn.codethink.xiaoming.common.RECEIPT_PACKET_FIELD_REQUEST
import cn.codethink.xiaoming.common.RECEIPT_PACKET_FIELD_STATE
import cn.codethink.xiaoming.common.REQUEST_PACKET_FIELD_ACTION
import cn.codethink.xiaoming.common.REQUEST_PACKET_FIELD_ARGUMENT
import cn.codethink.xiaoming.common.REQUEST_PACKET_FIELD_MODE
import cn.codethink.xiaoming.common.REQUEST_PACKET_FIELD_SUBJECT
import cn.codethink.xiaoming.common.REQUEST_PACKET_FIELD_TIMEOUT
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.currentTimeSeconds

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
    val id: String by raw
    val type: String by raw
    val time: Long by raw
    val cause: Cause? by raw

    @JvmOverloads
    constructor(
        id: String,
        type: String,
        time: Long = currentTimeSeconds,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PACKET_FIELD_ID] = id
        raw[PACKET_FIELD_TYPE] = type
        raw[PACKET_FIELD_TIME] = time
        raw[CAUSE_FIELD_CAUSE] = cause
    }
}

/**
 * Request packet represents a request from a software to another software.
 *
 * @author Chuanwise
 */
class RequestPacket(
    raw: Raw
) : Packet(raw) {
    val action: String by raw
    val mode: String by raw
    val argument: Any? by raw
    val subject: Subject? by raw
    val timeout: Long by raw

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
        raw[PACKET_FIELD_ID] = id
        raw[PACKET_FIELD_TYPE] = PACKET_TYPE_REQUEST
        raw[PACKET_FIELD_TIME] = time
        raw[CAUSE_FIELD_CAUSE] = cause
        raw[REQUEST_PACKET_FIELD_ACTION] = action
        raw[REQUEST_PACKET_FIELD_MODE] = mode
        raw[REQUEST_PACKET_FIELD_TIMEOUT] = timeout
        raw[REQUEST_PACKET_FIELD_ARGUMENT] = argument
        raw[REQUEST_PACKET_FIELD_SUBJECT] = subject
    }
}

/**
 * Receipt packet represents a response of a request packet.
 *
 * @author Chuanwise
 */
class ReceiptPacket(
    raw: Raw
) : Packet(raw) {
    val state: String by raw
    val request: String by raw
    val data: Any? by raw

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
        raw[PACKET_FIELD_ID] = id
        raw[PACKET_FIELD_TYPE] = PACKET_TYPE_RECEIPT
        raw[PACKET_FIELD_TIME] = time
        raw[CAUSE_FIELD_CAUSE] = cause
        raw[RECEIPT_PACKET_FIELD_STATE] = state
        raw[RECEIPT_PACKET_FIELD_REQUEST] = request
        raw[RECEIPT_PACKET_FIELD_DATA] = data
    }
}