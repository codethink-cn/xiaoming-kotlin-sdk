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

package cn.codethink.xiaoming.packet

import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.FIELD_TYPE
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MapRaw
import cn.codethink.xiaoming.util.Raw
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.getValue
import cn.codethink.xiaoming.util.nowTime
import cn.codethink.xiaoming.util.setValue
import com.fasterxml.jackson.annotation.JsonTypeName

const val RECEIPT_STATE_UNDEFINED = "undefined"
const val RECEIPT_STATE_SUCCEED = "succeed"
const val RECEIPT_STATE_FAILED = "failed"
const val RECEIPT_STATE_INTERRUPTED = "interrupted"

const val RECEIPT_STATE_RECEIVED = "received"
const val RECEIPT_STATE_CANCELLED = "cancelled"

private const val PACKET_TYPE_RECEIPT = "receipt"

@JsonTypeName(PACKET_TYPE_RECEIPT)
class ReceiptPacketImpl : AbstractBusinessPacket, ReceiptPacket {
    override var request: Id by raw
    override var state: String by raw
    override var data: Any? by raw
    override var cause: Cause? by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        id: Id,
        request: Id,
        state: String,
        data: Any?,
        session: SessionDescriptor,
        cause: Cause? = null,
        time: Time = nowTime,
        raw: Raw = MapRaw()
    ) : super(id, PACKET_TYPE_RECEIPT, time, session, raw) {
        this.request = request
        this.state = state
        this.data = data
        this.session = session
        this.cause = cause
    }
}