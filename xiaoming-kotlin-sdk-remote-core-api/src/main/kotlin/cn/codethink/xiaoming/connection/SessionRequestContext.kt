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

import cn.codethink.xiaoming.packet.ReceiptPacket
import cn.codethink.xiaoming.packet.RequestPacket
import cn.codethink.xiaoming.util.Received
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.NotStableForInheritance
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 表示会话接收到的一次请求。
 *
 * 会话请求上下文一旦创建，即表示请求开始执行。
 *
 * @author Chuanwise
 * @see Session.request
 */
@NotStableForInheritance
interface SessionRequestContext {
    val request: Received<RequestPacket>
    val receipt: ReceiptPacket

    val session: Session
}