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
import cn.codethink.xiaoming.util.AbstractData
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MutableStore
import cn.codethink.xiaoming.util.Store
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.property

abstract class AbstractEventOperation : AbstractData, EventOperation {
    private var type: String by raw.property()

    final override var time: Time by raw.property()
    final override var cause: Cause by raw.property()
    final override var listener: ListenerDescriptor by raw.property()

    @InternalApi
    constructor(raw: MutableStore) : super(raw)

    constructor(
        type: String,
        cause: Cause,
        listener: ListenerDescriptor,
        time: Time,
        raw: MutableStore
    ) : super(raw) {
        this.type = type
        this.cause = cause
        this.listener = listener
        this.time = time
    }
}