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

import cn.codethink.xiaoming.packet.ReceiptPacket
import cn.codethink.xiaoming.util.InternalImplementedApi
import java.util.concurrent.TimeUnit

@InternalImplementedApi
interface NotYetTerminatedRequestResult<T> : RequestResult<T> {
    val receipt: Received<ReceiptPacket>

    fun await(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): TerminatedRequestResult<T>?
    fun await(): TerminatedRequestResult<T>
}
