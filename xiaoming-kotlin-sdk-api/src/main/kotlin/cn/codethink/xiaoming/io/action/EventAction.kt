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

package cn.codethink.xiaoming.io.action

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.EVENT_SNAPSHOT_FIELD_CAUSE
import cn.codethink.xiaoming.common.EVENT_SNAPSHOT_FIELD_EVENT
import cn.codethink.xiaoming.common.EVENT_SNAPSHOT_FIELD_LISTENER
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.PUBLISH_EVENT_RECEIPT_DATA_SNAPSHOTS
import cn.codethink.xiaoming.common.PUBLISH_EVENT_REQUEST_PARA_EVENT
import cn.codethink.xiaoming.common.PUBLISH_EVENT_REQUEST_PARA_MUTABLE
import cn.codethink.xiaoming.common.PUBLISH_EVENT_REQUEST_PARA_TYPE
import cn.codethink.xiaoming.event.Event
import cn.codethink.xiaoming.event.listener.ListenerDescriptor
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set

class PublishEventRequestPara : AbstractData {
    val type: List<String> by raw
    val event: Event by raw
    val mutable: Boolean by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        type: List<String>,
        event: Event,
        mutable: Boolean = false,
        raw: Raw = MapRaw()
    ) : super(raw) {
        raw[PUBLISH_EVENT_REQUEST_PARA_TYPE] = type
        raw[PUBLISH_EVENT_REQUEST_PARA_EVENT] = event
        raw[PUBLISH_EVENT_REQUEST_PARA_MUTABLE] = mutable
    }
}

class EventSnapshot(
    raw: Raw
) : AbstractData(raw) {
    val event: Event by raw
    val listener: ListenerDescriptor by raw
    val cause: Cause? by raw

    @JvmOverloads
    constructor(
        event: Event,
        listener: ListenerDescriptor,
        cause: Cause?,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[EVENT_SNAPSHOT_FIELD_EVENT] = event
        raw[EVENT_SNAPSHOT_FIELD_LISTENER] = listener
        raw[EVENT_SNAPSHOT_FIELD_CAUSE] = cause
    }
}

class PublishEventReceiptData(
    raw: Raw
) : AbstractData(raw) {
    val snapshots: List<EventSnapshot> by raw

    @JvmOverloads
    constructor(
        snapshots: List<EventSnapshot> = emptyList(),
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PUBLISH_EVENT_RECEIPT_DATA_SNAPSHOTS] = snapshots
    }
}