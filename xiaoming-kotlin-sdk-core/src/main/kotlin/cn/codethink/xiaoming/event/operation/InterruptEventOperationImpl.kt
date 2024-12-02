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
import com.fasterxml.jackson.annotation.JsonTypeName

const val EVENT_OPERATION_TYPE_INTERRUPT = "interrupt"

@JsonTypeName(EVENT_OPERATION_TYPE_INTERRUPT)
class InterruptEventOperationImpl : AbstractEventOperation, InterruptEventOperation {
    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        cause: Cause,
        listener: ListenerDescriptor,
        time: Time,
        raw: Raw = MapRaw()
    ) : super(EVENT_OPERATION_TYPE_INTERRUPT, cause, listener, time, raw)
}