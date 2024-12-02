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

import cn.codethink.xiaoming.action.Action
import cn.codethink.xiaoming.packet.RequestMode
import cn.codethink.xiaoming.packet.SessionDescriptor
import cn.codethink.xiaoming.util.AutoClosableSubject
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.InternalImplementedApi
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.currentTimeSeconds
import kotlinx.coroutines.CoroutineScope
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 一个数据包会话，可以用于发送和接收请求。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
interface Session : AutoClosableSubject, CoroutineScope {
    /**
     * 会话描述符，描述会话的两端。
     */
    override val descriptor: SessionDescriptor

    /**
     * 会话的身份，和 [SessionDescriptor.to] 相同。
     */
    val subject: SubjectDescriptor

    /**
     * Stop 对应的是会话的关闭，而非连接的关闭。
     *
     * 有的实现会在无会话时自动关闭连接，但这不是 [Session] 使用者考虑的范畴。
     */
    val isStopped: Boolean

    @JvmBlockingBridge
    suspend fun <P, R> request(
        action: Action<P, R>,
        mode: RequestMode,
        argument: P?,
        timeout: Long,
        cause: Cause,
        subject: SubjectDescriptor,
        time: Long = currentTimeSeconds,
    ): RequestResult<R>
}
