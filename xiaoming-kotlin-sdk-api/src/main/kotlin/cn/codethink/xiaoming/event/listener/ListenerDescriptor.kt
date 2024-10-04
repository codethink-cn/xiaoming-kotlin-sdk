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

package cn.codethink.xiaoming.event.listener

import cn.codethink.xiaoming.common.ID_SUBJECT_DESCRIPTOR_DESCRIPTOR_FIELD_ID
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.IdSubjectDescriptor
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set

const val LISTENER_DESCRIPTOR_FIELD_SUBJECT = "subject"

/**
 * Describe a listener.
 *
 * @author Chuanwise
 */
class ListenerDescriptor(
    raw: Raw
) : IdSubjectDescriptor(raw) {
    val subject: SubjectDescriptor by raw

    constructor(
        id: Id,
        subject: SubjectDescriptor,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[ID_SUBJECT_DESCRIPTOR_DESCRIPTOR_FIELD_ID] = id
        raw[LISTENER_DESCRIPTOR_FIELD_SUBJECT] = subject
    }
}