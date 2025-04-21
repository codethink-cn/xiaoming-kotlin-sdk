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

import cn.codethink.xiaoming.packet.ReceiptPacket
import cn.codethink.xiaoming.util.Received
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.NotStableForInheritance

/**
 * 中止响应，表示一个动作的最终结果。
 *
 * @author Chuanwise
 * @see SucceedRequestResult
 * @see FailedRequestResult
 * @see InterruptedRequestResult
 */
@NotStableForInheritance
interface TerminatedRequestResult<T> : RequestResult<T> {
    val receipt: Received<ReceiptPacket>
}

@NotStableForInheritance
interface SucceedRequestResult<T> : TerminatedRequestResult<T> {
    val data: T
}

@NotStableForInheritance
interface FailedRequestResult<T> : TerminatedRequestResult<T> {
    val cause: Cause?
}

@NotStableForInheritance
interface InterruptedRequestResult<T> : TerminatedRequestResult<T> {
    val cause: Cause?
}

@NotStableForInheritance
interface CancelledRequestResult<T> : TerminatedRequestResult<T> {
    val cause: Cause?
}