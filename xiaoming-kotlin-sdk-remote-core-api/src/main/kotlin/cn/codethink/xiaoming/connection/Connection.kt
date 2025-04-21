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

import cn.codethink.xiaoming.util.Received
import cn.codethink.xiaoming.util.AutoClosableSubject
import cn.codethink.xiaoming.util.ConnectionDescriptor
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NotStableForInheritance
import cn.codethink.xiaoming.util.withTimeoutOrBool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import java.util.concurrent.TimeUnit

/**
 * 表示一个全双工的逻辑连接。可能是 [Client]
 *
 * @author Chuanwise
 */
@NotStableForInheritance
interface Connection<T> : AutoClosableSubject, CoroutineScope {
    /**
     * 连接描述符，与其权限有关。
     */
    override val descriptor: ConnectionDescriptor

    /**
     * 是否已经连接。
     */
    val isConnected: Boolean

    /**
     * 是否已经关闭。
     */
    val isClosed: Boolean
    val isClosing: Boolean
    val isClosingOrClosed: Boolean

    /**
     * 收消息通道。
     */
    val incoming: ReceiveChannel<Received<T>>

    /**
     * 发送数据。
     *
     * @param data 数据
     */
    @JvmBlockingBridge
    suspend fun send(data: T)

    /**
     * 接收数据。
     *
     * @param data 数据
     * @param origin 数据来源
     */
    @JvmBlockingBridge
    suspend fun receive(data: T, origin: Any?)
    suspend fun receive(data: T) = receive(data, origin = null)

    /**
     * 确保连接可用。
     *
     * @throws InterruptedException 等待连接可用过程被中断
     * @throws IllegalStateException 连接已经关闭
     */
    @JvmBlockingBridge
    @Throws(InterruptedException::class)
    suspend fun ensureConnected()

    /**
     * 确保连接可用。
     *
     * @param time 最长等待时间
     * @param unit 时间单位
     * @return 是否在等待时间内连接可用
     * @throws InterruptedException 等待过程中被中断
     * @throws IllegalStateException 连接已经关闭
     */
    @JvmBlockingBridge
    @OptIn(InternalApi::class)
    @Throws(InterruptedException::class)
    suspend fun ensureConnected(time: Long, unit: TimeUnit): Boolean {
        return withTimeoutOrBool(time, unit) { ensureConnected() }
    }

    /**
     * 确保连接可用。
     *
     * @param timeMillis 最长等待时间（毫秒）
     * @return 是否在等待时间内连接可用
     * @throws InterruptedException 等待过程中被中断
     * @throws IllegalStateException 连接已经关闭
     */
    @JvmBlockingBridge
    @OptIn(InternalApi::class)
    @Throws(InterruptedException::class)
    suspend fun ensureConnected(timeMillis: Long): Boolean {
        return withTimeoutOrBool(timeMillis) { ensureConnected() }
    }

    /**
     * 等待连接的状态发生变化。
     *
     * @param time 最长等待时间
     * @param unit 时间单位
     * @return 是否在等待时间内连接的状态发生了变化
     * @throws InterruptedException 等待过程中被中断
     */
    @JvmBlockingBridge
    @OptIn(InternalApi::class)
    @Throws(InterruptedException::class)
    suspend fun await(time: Long, unit: TimeUnit): Boolean {
        return withTimeoutOrBool(time, unit) { await() }
    }

    /**
     * 等待连接的状态发生变化。
     *
     * @param timeMillis 最长等待时间（毫秒）
     * @return 是否在等待时间内连接的状态发生了变化
     * @throws InterruptedException 等待过程中被中断
     */
    @JvmBlockingBridge
    @OptIn(InternalApi::class)
    @Throws(InterruptedException::class)
    suspend fun await(timeMillis: Long): Boolean {
        return withTimeoutOrBool(timeMillis) { await() }
    }

    /**
     * 等待连接的状态发生变化。
     *
     * @throws InterruptedException 等待过程中被中断
     * @throws IllegalStateException 连接已经关闭
     */
    @JvmBlockingBridge
    @Throws(InterruptedException::class)
    suspend fun await()
}