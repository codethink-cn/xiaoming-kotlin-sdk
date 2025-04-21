/*
 * Copyright 2025 CodeThink Technologies and contributors.
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
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MutableStore
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.createMapStore
import cn.codethink.xiaoming.util.nowTime
import cn.codethink.xiaoming.util.property
import com.fasterxml.jackson.annotation.JsonTypeName

const val RECEIPT_PACKET_FIELD_DATA = "data"

private const val PACKET_TYPE_RECEIPT = "receipt"

@JsonTypeName(PACKET_TYPE_RECEIPT)
class ReceiptPacketImpl : AbstractBusinessPacket, ReceiptPacket {
    override var request: Id by raw.property()
    override var state: ReceiptState by raw.property()
    override var data: Any? by raw.property()
    override var cause: Cause? by raw.property()

    override val description: String = "Receipt packet to request $request ($id)"

    @InternalApi
    constructor(raw: MutableStore) : super(raw)

    @JvmOverloads
    constructor(
        id: Id,
        request: Id,
        state: ReceiptState,
        data: Any?,
        session: SessionDescriptor,
        cause: Cause? = null,
        time: Time = nowTime,
        raw: MutableStore = createMapStore()
    ) : super(id, PACKET_TYPE_RECEIPT, time, session, raw) {
        this.request = request
        this.state = state
        this.data = data
        this.session = session
        this.cause = cause
    }
}