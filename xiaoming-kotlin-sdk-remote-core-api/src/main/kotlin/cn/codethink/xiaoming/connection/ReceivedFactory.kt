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

@file:JvmName("ReceivedFactory")

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.api.RemoteCoreApi
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Received

@JvmOverloads
@OptIn(InternalApi::class)
@JvmName("createReceived")
fun <T> Received(data: T, origin: Any? = null): Received<T> {
    return RemoteCoreApi.getInstance().createReceived(data, origin)
}