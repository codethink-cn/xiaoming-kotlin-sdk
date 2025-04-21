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

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.packet.SessionPacket
import cn.codethink.xiaoming.util.Received
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.NotStableForInheritance
import cn.codethink.xiaoming.util.SubjectDescriptor
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 会话握手上下文。
 *
 * @author Chuanwise
 * @see SessionActiveStartContext
 * @see SessionPassiveHandshakeContext
 */
@NotStableForInheritance
interface SessionHandshakeContext : SessionContext {
    val received: Received<SessionPacket>

    /**
     * 拒绝连接。
     *
     * @param cause 拒绝原因
     * @param subject 拒绝主体，`null` 表示处理器注册方
     */
    @JvmBlockingBridge
    suspend fun reject(cause: Cause, subject: SubjectDescriptor?)

    /**
     * 拒绝连接。
     *
     * @param cause 拒绝原因
     */
    @JvmBlockingBridge
    suspend fun reject(cause: Cause) = reject(cause, subject = null)
}