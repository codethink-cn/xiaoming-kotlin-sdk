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

import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.NotStableForInheritance
import cn.codethink.xiaoming.util.SubjectDescriptor
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 己方被动接收对方的会话请求时的上下文。
 *
 * @author Chuanwise
 */
@NotStableForInheritance
interface SessionPassiveHandshakeContext<T> : SessionHandshakeContext {
    /**
     * 接收连接。
     *
     * 接受连接后，将会给对方回应并等待对方响应。若对方最终同意，则函数返回，否则抛出异常。
     *
     * @param data 发送给对方的数据
     * @param handler 请求处理器
     * @param cause 同意原因
     * @param subject 同意主体，默认为处理器注册方
     * @return 会话
     * @throws SessionClosedException 会话被关闭
     * @throws InterruptedException 等待过程中被中断
     * @see SessionClosedException
     */
    @JvmBlockingBridge
    suspend fun accept(
        data: T,
        handler: SessionHandler,
        cause: Cause? = null,
        subject: SubjectDescriptor? = null
    ): Session
}