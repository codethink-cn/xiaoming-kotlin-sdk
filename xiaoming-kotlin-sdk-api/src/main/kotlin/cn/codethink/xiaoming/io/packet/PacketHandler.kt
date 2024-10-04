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

package cn.codethink.xiaoming.io.packet

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.CauseSubjectPair
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.io.ProtocolLanguageConfiguration
import cn.codethink.xiaoming.io.connection.ConnectionApi
import cn.codethink.xiaoming.io.connection.PacketConnection
import cn.codethink.xiaoming.io.connection.Received
import io.github.oshai.kotlinlogging.KLogger

data class DisconnectSetting(
    val cause: Cause,
    val subject: SubjectDescriptor
)

/**
 * The context of a packet.
 *
 * @author Chuanwise
 */
data class PacketContext(
    val logger: KLogger,
    val connection: PacketConnection<*>,
    private val connectionApi: ConnectionApi<*>,

    val received: Received<*>,
    val language: ProtocolLanguageConfiguration,

    var disconnect: CauseSubjectPair? = null
) {
    suspend fun send(packet: Packet) {
        connectionApi.send(packet)
    }
}

interface PacketHandler {
    suspend fun handle(context: PacketContext)
}
