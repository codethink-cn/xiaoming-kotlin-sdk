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

package cn.codethink.xiaoming.common

import cn.codethink.xiaoming.event.Event
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set
import cn.codethink.xiaoming.io.data.setValue
import cn.codethink.xiaoming.io.packet.Packet
import com.fasterxml.jackson.annotation.JsonTypeName

/**
 * Cause is the reason of an operation, an error, an event, etc.
 *
 * @author Chuanwise
 */
abstract class Cause(
    raw: Raw
) : AbstractData(raw) {
    val type: String by raw
    open var cause: Cause? by raw

    @JvmOverloads
    constructor(
        type: String,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = type
        raw[CAUSE_FIELD_CAUSE] = cause
    }
}

/**
 * Represent a cause that is a text.
 *
 * @author Chuanwise
 */
open class TextCause(
    raw: Raw
) : Cause(raw) {
    var text: String by raw

    @JvmOverloads
    constructor(
        text: String,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = CAUSE_TYPE_TEXT

        this.cause = cause
        raw[CAUSE_FIELD_CAUSE] = cause
        raw[TEXT_CAUSE_FIELD_TEXT] = text
    }
}

/**
 * Represent a cause that is a known packet.
 *
 * @author Chuanwise
 */
abstract class PacketCause(
    raw: Raw
) : Cause(raw) {
    open val id: String by raw

    @JvmOverloads
    constructor(
        id: String,
        type: String,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = type
        raw[CAUSE_FIELD_CAUSE] = cause
        raw[PACKET_CAUSE_FIELD_ID] = id
    }
}

/**
 * Represent a packet referenced by its id. Receiver should find the recent packet
 * cache and replace it to [PacketDataCause] (if present) and pass to downstream.
 *
 * @author Chuanwise
 */
class PacketIdCause(
    raw: Raw
) : PacketCause(raw) {
    @JvmOverloads
    constructor(
        id: String,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = CAUSE_TYPE_PACKET_ID
        raw[CAUSE_FIELD_CAUSE] = cause
        raw[PACKET_CAUSE_FIELD_ID] = id
    }
}

/**
 * Represent a packet that is the cause of the operation.
 *
 * @author Chuanwise
 */
class PacketDataCause(
    raw: Raw
) : PacketCause(raw) {
    val packet: Packet by raw

    override val id: String by packet::id
    override var cause: Cause? by packet::cause

    @JvmOverloads
    constructor(
        packet: Packet,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = CAUSE_TYPE_PACKET_DATA
        raw[PACKET_DATA_CAUSE_FIELD_PACKET] = packet
    }
}


/**
 * Compared to the traditional [TextCause], this class provides [id] and [context]
 * that are easier for programs to recognize, so that relevant solutions can be provided
 * based on them. At the same time, it also provides [text] that is easy for humans
 * to read.
 *
 * @author Chuanwise
 */
class StandardTextCause(
    raw: Raw
) : Cause(raw) {
    var id: String by raw
    var text: String by raw
    var context: Map<String, Any?> by raw

    @JvmOverloads
    constructor(
        id: String,
        text: String,
        context: Map<String, Any?>,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = CAUSE_TYPE_STANDARD_TEXT

        this.id = id
        this.text = text
        this.context = context
    }
}

const val CAUSE_TYPE_EVENT = "event"

/**
 * Represent a cause that is an event.
 *
 * In the most cases, developers should use [PacketCause].
 *
 * @author Chuanwise
 */
@JsonTypeName(CAUSE_TYPE_EVENT)
class EventCause(
    raw: Raw
) : Cause(raw) {
    var event: Event by raw

    @JvmOverloads
    constructor(
        event: Event,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = CAUSE_TYPE_EVENT

        this.event = event
        this.cause = cause
    }
}