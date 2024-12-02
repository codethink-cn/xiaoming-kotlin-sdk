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

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.api

import cn.codethink.xiaoming.connection.Received
import cn.codethink.xiaoming.connection.ReceivedImpl
import cn.codethink.xiaoming.util.InternalApi

class RemoteCoreApiImpl : RemoteCoreApi {
    override fun <T> createReceived(origin: Any?, data: T): Received<T> {
        return ReceivedImpl(origin, data)
    }
}