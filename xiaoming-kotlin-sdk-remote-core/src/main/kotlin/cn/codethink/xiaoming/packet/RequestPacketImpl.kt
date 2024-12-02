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
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MapRaw
import cn.codethink.xiaoming.util.Raw
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.getValue
import cn.codethink.xiaoming.util.nowTime
import cn.codethink.xiaoming.util.setValue
import com.fasterxml.jackson.annotation.JsonTypeName

const val PACKET_TYPE_REQUEST = "request"

@JsonTypeName(PACKET_TYPE_REQUEST)
class RequestPacketImpl : AbstractBusinessPacket, RequestPacket {
    override var action: Id by raw
    override var mode: String by raw
    override var argument: Any? by raw
    override var timeout: Long by raw
    override var subject: SubjectDescriptor by raw
    override var cause: Cause by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        id: Id,
        action: Id,
        mode: String,
        argument: Any?,
        timeout: Long,
        subject: SubjectDescriptor,
        session: SessionDescriptor,
        cause: Cause,
        time: Time = nowTime,
        raw: Raw = MapRaw()
    ) : super(id, PACKET_TYPE_REQUEST, time, session, raw) {
        this.action = action
        this.mode = mode
        this.argument = argument
        this.timeout = timeout
        this.subject = subject
        this.session = session
        this.cause = cause
    }
}
