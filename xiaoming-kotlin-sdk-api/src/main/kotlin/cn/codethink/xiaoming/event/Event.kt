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

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.event

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.EventCause
import cn.codethink.xiaoming.common.FIELD_CAUSE
import cn.codethink.xiaoming.common.FIELD_TYPE
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set

/**
 * Represent an event that can be listened by listeners and published by subjects.
 *
 * @author Chuanwise
 */
abstract class Event : AbstractData {
    val type: String by raw
    val cause: Cause by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        type: String,
        cause: Cause,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = type
        raw[FIELD_CAUSE] = cause
    }
}

val Event.subject
    get() = cause.subject

fun Event.toCause() = EventCause(this)
