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

import cn.codethink.xiaoming.packet.ReceiptPacket
import cn.codethink.xiaoming.packet.RequestMode
import cn.codethink.xiaoming.packet.RequestPacket
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.InternalImplementedApi
import cn.codethink.xiaoming.util.SubjectDescriptor
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 表示会话接收到的一次请求。
 *
 * 会话请求上下文一旦创建，即表示请求开始执行。
 *
 * @author Chuanwise
 * @see Session.handle
 */
@InternalImplementedApi
interface SessionRequestContext {
    val request: Received<RequestPacket>
    val receipt: ReceiptPacket

    val mode: RequestMode
    val session: Session

    val isReceived: Boolean
    val isOperated: Boolean
    val isSucceed: Boolean
    val isFailed: Boolean
    val isInterrupted: Boolean

    @JvmBlockingBridge
    suspend fun received()

    @JvmBlockingBridge
    suspend fun succeed(data: Any? = null)

    @JvmBlockingBridge
    suspend fun failed(cause: Cause)

    /**
     * 中断请求处理。
     *
     * @author Chuanwise
     */
    @JvmBlockingBridge
    suspend fun interrupt(cause: Cause)
}

/**
 * 会话请求处理器。
 *
 * 请求处理器应当在请求相关参数无误后调用 [SessionRequestContext.received] 以开始处理请求。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
interface SessionRequestHandler {
    @JvmBlockingBridge
    suspend fun handle(context: SessionRequestContext)
}