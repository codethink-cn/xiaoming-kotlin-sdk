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

@file:JvmName("ConnectionApiKt")

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.util.AutoClosableSubject
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.ConnectionDescriptor
import cn.codethink.xiaoming.util.SubjectDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import java.util.concurrent.TimeUnit
import kotlin.time.Duration


/**
 * 连接 API，表示一个面向数据帧的全双工连接。
 *
 * @param T 数据帧类型。
 * @author Chuanwise
 * @see LongConnectionApi
 */
sealed interface ConnectionApi<T> : AutoClosableSubject, CoroutineScope, AutoCloseable {
    val isClosed: Boolean

    @JvmBlockingBridge
    suspend fun send(data: T)

    /**
     * 表示接收到某一数据，其只是将数据转发到接受管道。
     */
    @JvmBlockingBridge
    suspend fun receive(data: T, origin: Any? = null)

    /**
     * 接收管道。
     */
    val channel: Channel<Received<T>>

    /**
     * 重新建立连接，通常用于看门狗发现连接超时时。
     */
    suspend fun restart(cause: Cause, subject: SubjectDescriptor)

    /**
     * Await for state change.
     *
     * @param time The time to wait.
     * @param unit The unit of the time.
     */
    fun await(time: Long, unit: TimeUnit): Boolean

    /**
     * Await for state change.
     */
    fun await()
}

fun ConnectionApi<*>.await(duration: Duration) = await(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
fun ConnectionApi<*>.await(timeMillis: Long) = await(timeMillis, TimeUnit.MILLISECONDS)