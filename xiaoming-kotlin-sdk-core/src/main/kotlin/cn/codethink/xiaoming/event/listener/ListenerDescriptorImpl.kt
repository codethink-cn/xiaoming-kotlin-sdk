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

package cn.codethink.xiaoming.event.listener

import cn.codethink.xiaoming.util.AbstractData
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MutableStore
import cn.codethink.xiaoming.util.SegmentId
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.createMapStore
import cn.codethink.xiaoming.util.property

/**
 * Describe a listener.
 *
 * @author Chuanwise
 */
class ListenerDescriptorImpl : AbstractData, ListenerDescriptor {
    override var id: SegmentId by raw.property()
    override var subject: SubjectDescriptor by raw.property()

    @InternalApi
    constructor(raw: MutableStore) : super(raw)

    @JvmOverloads
    constructor(
        id: SegmentId,
        subject: SubjectDescriptor,
        raw: MutableStore = createMapStore()
    ) : super(raw) {
        this.id = id
        this.subject = subject
    }
}