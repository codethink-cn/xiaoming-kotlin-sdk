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

package cn.codethink.xiaoming.io.connection

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.TextCause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.TimeUnit
import kotlin.time.Duration


/**
 * Connection internal API represents a real connection. Its responsibility is to
 * manage the connection state, sending and receiving messages.
 *
 * @param T data type.
 * @author Chuanwise
 * @see LongConnectionInternalApi
 */
sealed interface ConnectionInternalApi<T> : CoroutineScope, AutoCloseable {
    /**
     * Represent the connection internal API itself.
     */
    val subjectDescriptor: SubjectDescriptor

    /**
     * Whether the connection is closed.
     */
    val isClosed: Boolean

    /**
     * Send data to the other side.
     */
    suspend fun send(data: T)

    /**
     * Receive data from the other side. It just forward the data to the channel.
     */
    suspend fun receive(data: T)

    /**
     * Receiving channel.
     */
    val channel: Channel<T>

    /**
     * Close the connection.
     */
    fun close(cause: Cause, subjectDescriptor: SubjectDescriptor)

    override fun close() = close(TextCause("Connection internal API closed."), subjectDescriptor)

    /**
     * Await for state change.
     *
     * @param time The time to wait.
     * @param unit The unit of the time.
     */
    fun await(time: Long, unit: TimeUnit) : Boolean

    /**
     * Await for state change.
     */
    fun await()
}

fun ConnectionInternalApi<*>.await(duration: Duration) = await(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
fun ConnectionInternalApi<*>.await(timeMillis: Long) = await(timeMillis, TimeUnit.MILLISECONDS)