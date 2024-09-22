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

import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.io.connection.LocalPlatformWebSocketServer
import cn.codethink.xiaoming.io.connection.Server
import cn.codethink.xiaoming.io.connection.WebSocketServerConfiguration


const val SERVER_CONFIGURATION_TYPE_WEBSOCKET = "web_socket"

class LocalPlatformWebSocketServerConfiguration(
    override val port: Int,
    override val host: String?,
    override val path: String,
    override val subject: Subject,
    override val enable: Boolean = true
) : ServerConfiguration, WebSocketServerConfiguration {
    override val type: String = SERVER_CONFIGURATION_TYPE_WEBSOCKET

    override fun toServer(api: LocalPlatformInternalApi): Server {
        return LocalPlatformWebSocketServer(
            configuration = this,
            internalApi = api,
            authorizer = api.connectionManagerApi.authorizer,
            subject = subject,
            parentJob = api.supervisorJob,
            parentCoroutineContext = api.coroutineContext
        )
    }
}