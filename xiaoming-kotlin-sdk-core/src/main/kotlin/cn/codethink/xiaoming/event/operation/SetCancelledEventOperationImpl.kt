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

package cn.codethink.xiaoming.event.operation

import cn.codethink.xiaoming.event.listener.ListenerDescriptor
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MapRaw
import cn.codethink.xiaoming.util.Raw
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.getValue
import cn.codethink.xiaoming.util.setValue
import com.fasterxml.jackson.annotation.JsonTypeName

const val EVENT_OPERATION_TYPE_SET_CANCELLED = "set_cancelled"

@JsonTypeName(EVENT_OPERATION_TYPE_SET_CANCELLED)
class SetCancelledEventOperationImpl : AbstractEventOperation, SetCancelledEventOperation {
    override var cancelled: Boolean by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        cancelled: Boolean,
        cause: Cause,
        listener: ListenerDescriptor,
        time: Time,
        raw: Raw = MapRaw()
    ) : super(
        EVENT_OPERATION_TYPE_SET_CANCELLED, cause, listener, time, raw
    ) {
        this.cancelled = cancelled
    }
}