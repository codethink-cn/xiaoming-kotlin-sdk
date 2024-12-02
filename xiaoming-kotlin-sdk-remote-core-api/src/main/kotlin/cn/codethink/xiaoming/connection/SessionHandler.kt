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

import cn.codethink.xiaoming.packet.MetaPacket
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.InternalImplementedApi
import cn.codethink.xiaoming.util.RawFieldType
import cn.codethink.xiaoming.util.SubjectDescriptor
import kotlinx.coroutines.TimeoutCancellationException
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

interface SessionContext {
    val connection: Connection
}

/**
 * 和会话启动相关的上下文。
 *
 * 会话建立时，主动建立方调用 [Connection.start] 向被动方发送会话启动请求。
 * 被动方的 [SessionHandler.onBackwardStart] 将被调用。
 * 若拒绝，则握手结束，主动方的 [Connection.start] 抛出异常 [SessionForwardRejectedException]。
 *
 * 若被动方同意，视为主动方向被动方的会话可以建立，但被动方向主动方的会话尚未建立。
 *
 * 在被动方发送给主动方的同意数据包中，携带被动方的介绍。主动方的 [SessionHandler.onForwardStart]
 * 将被调用。若拒绝，则被动方的 [SessionBackwardStartContext.accept] 抛出 [SessionForwardStartContext]
 * 异常，主动方的 [Connection.start] 抛出 [SessionBackwardRejectedException] 异常。
 *
 * @author Chuanwise
 * @see SessionForwardStartContext
 * @see SessionBackwardStartContext
 */
@InternalImplementedApi
interface SessionStartContext : SessionContext {
    val packet: Received<MetaPacket>

    val isCurrentSideOperated: Boolean
    val isCurrentSideAccepted: Boolean
    val isCurrentSideRejected: Boolean

    val isOtherSideOperated: Boolean
    val isOtherSideAccepted: Boolean
    val isOtherSideRejected: Boolean

    /**
     * 拒绝连接。
     *
     * @param cause 拒绝原因
     * @param subject 拒绝主体，默认为处理器注册方
     */
    fun reject(cause: Cause, subject: SubjectDescriptor? = null)
}

/**
 * 己方主动请求打开和对方的会话时的上下文。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
interface SessionForwardStartContext<T> : SessionStartContext {
    /**
     * 对方发送的数据。
     */
    val data: T

    /**
     * 接收连接。
     *
     * @param handler 请求处理器
     * @param cause 同意原因
     * @param subject 同意主体，默认为处理器注册方
     * @return 会话
     * @throws SessionRejectedException 对方拒绝连接
     * @throws TimeoutCancellationException 对方响应超时
     */
    fun accept(handler: SessionRequestHandler, cause: Cause? = null, subject: SubjectDescriptor? = null): Session
}

@InternalImplementedApi
interface SessionBackwardStartContext<T> : SessionStartContext {
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
     * @throws SessionRejectedException 对方拒绝连接
     * @throws TimeoutCancellationException 对方响应超时
     */
    @JvmBlockingBridge
    suspend fun accept(
        data: T,
        handler: SessionRequestHandler,
        cause: Cause? = null,
        subject: SubjectDescriptor? = null
    ): Session
}

@InternalImplementedApi
interface SessionStopContext {
    val session: Session

    /**
     * 空表示己方主动关闭，非空表示是收到对方关闭数据包才开始关闭。
     */
    val packet: Received<MetaPacket>?

    val cause: Cause
    val subject: SubjectDescriptor
}

/**
 * 在一个可以复用的连接上收到连接数据包时，表示对方希望建立一个连接。
 *
 * @author Chuanwise
 * @see SessionStartContext
 */
interface SessionHandler<P, R> {
    val forwardDataType: RawFieldType<P>
    val backwardDataType: RawFieldType<R>

    fun onForwardStart(context: SessionForwardStartContext<P>)
    fun onBackwardStart(context: SessionBackwardStartContext<R>)

    fun onStop(context: SessionStopContext)
}