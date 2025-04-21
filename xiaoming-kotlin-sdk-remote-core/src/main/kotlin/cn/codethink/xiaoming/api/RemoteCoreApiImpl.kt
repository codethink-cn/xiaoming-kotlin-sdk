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

import cn.codethink.xiaoming.connection.MaxAttemptAndDelayAutoReconnectPolicyImpl
import cn.codethink.xiaoming.util.Received
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NoOriginReceivedImpl
import cn.codethink.xiaoming.util.ReceivedImpl
import kotlin.time.Duration

class RemoteCoreApiImpl : RemoteCoreApi {
    // Received
    override fun <T> createReceived(data: T, origin: Any?): Received<T> {
        return if (origin == null) {
            NoOriginReceivedImpl(data)
        } else {
            ReceivedImpl(data, origin)
        }
    }

    // AutoReconnectPolicy
    override fun createMaxAttemptAndDelayAutoReconnectPolicy(maxAttempt: Int, delay: Duration): cn.codethink.xiaoming.connection.AutoReconnectPolicy {
        return MaxAttemptAndDelayAutoReconnectPolicyImpl(maxAttempt, delay)
    }
}