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

@file:JvmName("RequestResults")

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.packet.RequestPacket
import cn.codethink.xiaoming.util.InternalImplementedApi

/**
 * 表示一次请求的结果。
 *
 * @author Chuanwise
 * @see Session.request
 * @see TerminatedRequestResult
 * @see NotYetTerminatedRequestResult
 */
@InternalImplementedApi
interface RequestResult<T> {
    val session: Session
    val request: RequestPacket
}

val RequestResult<*>.isTerminated: Boolean
    get() = this is TerminatedRequestResult<*>

/**
 * 表示对方收到了请求，但是是否执行，以及执行结果如何，都不确定。
 *
 * 根据不同的请求模式，此结果可能是终止响应，也可能是非终止响应。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
interface ReceivedRequestResult<T> : RequestResult<T>