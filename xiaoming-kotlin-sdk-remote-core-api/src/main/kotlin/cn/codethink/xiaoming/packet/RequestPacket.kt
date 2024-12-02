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

import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalImplementedApi
import cn.codethink.xiaoming.util.SubjectDescriptor

const val REQUEST_PACKET_FIELD_ARGUMENT = "argument"

/**
 * 请求数据包。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
interface RequestPacket : BusinessPacket {
    val action: Id
    val mode: String
    val argument: Any?
    val timeout: Long
    val subject: SubjectDescriptor
    override val cause: Cause
}