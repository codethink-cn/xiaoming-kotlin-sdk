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

import cn.codethink.xiaoming.common.Subject
import io.github.oshai.kotlinlogging.KLogger
import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.netty.Netty
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

interface RemoteWebSocketServerConfiguration : WebSocketServerConfiguration


class RemoteWebSocketServer(
    private val configuration: WebSocketServerConfiguration,
    private val logger: KLogger,
    override val subject: Subject,
    applicationEngineFactory: ApplicationEngineFactory<*, *> = Netty,
    parentJob: Job? = null,
    parentCoroutineContext: CoroutineContext = Dispatchers.IO
) : WebSocketServer(
    configuration, logger, subject, applicationEngineFactory, parentJob, parentCoroutineContext
) {
    private val mutableConnections: MutableList<FrameConnection> = CopyOnWriteArrayList()
    override val connections: List<FrameConnection> = mutableConnections.toList()

    override suspend fun PipelineContext<Unit, ApplicationCall>.onConnect() {
        TODO("Not yet implemented")
    }

    override suspend fun WebSocketServerSession.onConnected(parentJob: Job, parentCoroutineContext: CoroutineContext) {
        TODO("Not yet implemented")
    }
}