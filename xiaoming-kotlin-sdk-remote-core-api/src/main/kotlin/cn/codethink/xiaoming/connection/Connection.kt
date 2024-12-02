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

import cn.codethink.xiaoming.packet.Packet
import cn.codethink.xiaoming.packet.SessionDescriptor
import cn.codethink.xiaoming.util.AutoClosableSubject
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.ConnectionDescriptor
import cn.codethink.xiaoming.util.InternalImplementedApi
import cn.codethink.xiaoming.util.SubjectDescriptor
import kotlinx.coroutines.CoroutineScope
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 表示一个连接，可以复用。
 *
 * 在连接上注册对象以接收连接请求。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
interface Connection : AutoClosableSubject, CoroutineScope {
    override val descriptor: ConnectionDescriptor

    val isClosed: Boolean
    val isShared: Boolean

    val sessions: Collection<Session>

    /**
     * 注册一个监听器处理连接请求。
     */
    fun handle(type: String, to: String, handler: SessionHandler<*, *>, subject: SubjectDescriptor)

    /**
     * 尝试启动一个新的会话。
     *
     * @author Chuanwise
     */
    @JvmBlockingBridge
    suspend fun start(data: Any?, session: SessionDescriptor, cause: Cause): Session

    @JvmBlockingBridge
    suspend fun send(packet: Packet)

    @JvmBlockingBridge
    suspend fun receive(packet: Packet, origin: Any? = null)
}
