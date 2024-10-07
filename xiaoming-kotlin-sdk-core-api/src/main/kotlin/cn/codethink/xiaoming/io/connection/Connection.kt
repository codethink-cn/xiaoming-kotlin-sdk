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

import cn.codethink.xiaoming.common.AutoClosableSubject
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.currentTimeSeconds
import cn.codethink.xiaoming.io.action.Action
import kotlinx.coroutines.CoroutineScope

/**
 * Used to do request and get its response.
 *
 * @author Chuanwise
 * @see PacketConnection
 */
interface Connection<T> : AutoClosableSubject, CoroutineScope {
    val session: SubjectDescriptor
    val isClosed: Boolean

    suspend fun <P, R> request(
        action: Action<P, R>,
        mode: String,
        timeout: Long,
        cause: Cause,
        argument: P? = null,
        time: Long = currentTimeSeconds,
    ) : Pair<Received<T>, R?>
}

val Connection<*>.isNotClosed: Boolean
    get() = !isClosed