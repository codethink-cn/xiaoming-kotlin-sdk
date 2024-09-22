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
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.TextCause
import kotlinx.coroutines.CoroutineScope

/**
 * Server may hold multiple connections.
 *
 * @author Chuanwise
 * @see WebSocketServer
 */
interface Server : AutoCloseable, CoroutineScope {
    val connections: List<Connection>
    val isStarted: Boolean
    val isClosed: Boolean
    val subject: Subject

    fun close(cause: Cause, subject: Subject)
    override fun close() = close(TextCause("Server closed."), subject)
}