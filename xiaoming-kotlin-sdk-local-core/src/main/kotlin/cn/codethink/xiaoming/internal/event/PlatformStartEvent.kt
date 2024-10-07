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

package cn.codethink.xiaoming.internal.event

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.InternalEvent
import cn.codethink.xiaoming.event.Event
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw

const val EVENT_TYPE_PLATFORM_STARTING = "platform_starting"

/**
 * Create and passed by the platform when it is starting, to notify all
 * modules that the platform is starting.
 *
 * @author Chuanwise
 */
@InternalEvent
class PlatformStartEvent : Event {
    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        cause: Cause,
        raw: Raw = MapRaw()
    ) : super(
        type = EVENT_TYPE_PLATFORM_STARTING,
        cause = cause,
        raw = raw
    )
}