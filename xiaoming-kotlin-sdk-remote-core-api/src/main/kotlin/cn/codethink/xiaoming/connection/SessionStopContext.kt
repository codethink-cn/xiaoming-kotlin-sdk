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

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.packet.SessionPacket
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.NotStableForInheritance
import cn.codethink.xiaoming.util.SubjectDescriptor

/**
 * 会话停止上下文。
 *
 * @author Chuanwise
 * @see SessionActiveStopContext
 * @see SessionPassiveStopContext
 */
@NotStableForInheritance
interface SessionStopContext : SessionContext {
    val session: Session
    val packet: SessionPacket

    val cause: Cause
    val subject: SubjectDescriptor
}