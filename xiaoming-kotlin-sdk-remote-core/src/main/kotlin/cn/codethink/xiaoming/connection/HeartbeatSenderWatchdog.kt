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

import cn.codethink.xiaoming.packet.META_PACKET_ACTION_HEARTBEAT
import cn.codethink.xiaoming.packet.MetaPacket
import cn.codethink.xiaoming.packet.MetaPacketImpl
import cn.codethink.xiaoming.packet.Packet
import cn.codethink.xiaoming.util.createRandomUniversalUniqueId

class HeartbeatSenderWatchdog : HeartbeatWatchdog {
    override suspend fun onTimeout(connection: Connection) {
        val packet = MetaPacketImpl(
            id = createRandomUniversalUniqueId(),
            action = META_PACKET_ACTION_HEARTBEAT,
            data = null
        )

        connection.send(packet)
    }

    override suspend fun onFeed(connection: Connection, received: Received<Packet>) {
        error("HeartbeatSenderWatchdog should not receive any packet.")
    }
}