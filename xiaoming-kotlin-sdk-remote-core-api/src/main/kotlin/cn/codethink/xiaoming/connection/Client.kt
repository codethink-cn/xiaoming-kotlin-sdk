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

import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.withTimeoutOrBool
import kotlinx.coroutines.withTimeoutOrNull
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws

/**
 * 客户端，表示一个连接的发起者。
 *
 * @author Chuanwise
 */
interface Client<T> : Connection<T> {
    val isConnecting: Boolean

    /**
     * 连接到服务端。
     *
     * @throws InterruptedException 等待过程中被中断
     * @throws IllegalStateException 连接被关闭或已关闭
     */
    @JvmBlockingBridge
    @Throws(InterruptedException::class)
    suspend fun connect()

    /**
     * 连接到服务端。
     *
     * @param time 最长等待时间
     * @param unit 时间单位
     * @return 是否在等待时间内连接成功
     * @throws InterruptedException 等待过程中被中断
     * @throws IllegalStateException 连接被关闭或已关闭
     */
    @JvmBlockingBridge
    @Throws(InterruptedException::class)
    suspend fun connect(time: Long, unit: TimeUnit): Boolean = connect(unit.toMillis(time))

    /**
     * 连接到服务端。
     *
     * @param timeMillis 最长等待时间（毫秒）
     * @return 是否在等待时间内连接成功
     * @throws InterruptedException 等待过程中被中断
     * @throws IllegalStateException 连接被关闭或已关闭
     */
    @JvmBlockingBridge
    @OptIn(InternalApi::class)
    @Throws(InterruptedException::class)
    suspend fun connect(timeMillis: Long): Boolean {
        return withTimeoutOrBool(timeMillis) { connect() }
    }
}