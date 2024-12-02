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

import cn.codethink.xiaoming.packet.MetaPacket
import cn.codethink.xiaoming.packet.Packet

/**
 * 心跳包看门狗。
 *
 * 每次收到数据包时，[onFeed] 将会被调用。
 * 如果超过给定的最大时长仍未收到任何数据包，[onTimeout] 将被调用。
 *
 * 请注意对应数据包不一定是心跳包，可能是其他数据包。
 * 但是总的原则是如果收到了数据包，那么一段时间内就不会触发超时。
 *
 * @author Chuanwise
 */
interface HeartbeatWatchdog {
    suspend fun onTimeout(connection: Connection)
    suspend fun onFeed(connection: Connection, received: Received<Packet>)
}