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

import cn.codethink.xiaoming.util.SubjectDescriptor
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeName

private const val SUBJECT_DESCRIPTOR_TYPE_SESSION = "session"

@JsonTypeName(SUBJECT_DESCRIPTOR_TYPE_SESSION)
data class SessionDescriptorImpl(
    override val from: SubjectDescriptor,
    override val to: SubjectDescriptor,
) : SessionDescriptor {
    override val type: String = SUBJECT_DESCRIPTOR_TYPE_SESSION
}

val SessionDescriptor.reversed: SessionDescriptor
    get() = SessionDescriptorImpl(to, from)