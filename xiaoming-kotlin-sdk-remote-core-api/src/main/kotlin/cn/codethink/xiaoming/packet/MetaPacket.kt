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

package cn.codethink.xiaoming.packet

import cn.codethink.xiaoming.util.InternalImplementedApi
import cn.codethink.xiaoming.util.SubjectDescriptor

const val META_PACKET_ACTION_START = "start"
const val META_PACKET_ACTION_STARTED = "started"

const val META_PACKET_ACTION_STOP = "stop"
const val META_PACKET_ACTION_STOPPED = "stopped"

const val META_PACKET_ACTION_HEARTBEAT = "heartbeat"

/**
 * 元数据包。
 *
 * 在会话建立时，双方向对方发送 [action] 为 [META_PACKET_ACTION_START] 的连接数据包。
 * 对方可能回复 [META_PACKET_ACTION_STARTED] 或 [META_PACKET_ACTION_STOPPED]。
 * 只有双方都在指定时间内回复 [META_PACKET_ACTION_STARTED] 才认为连接建立，否则切断连接。
 *
 * 断开连接时，主动断开方向对方发送 [action] 为 [META_PACKET_ACTION_STOP] 的断连数据包。
 * 对方只能回复 [META_PACKET_ACTION_STOPPED]，或超时。此时连接断开。
 *
 * 此外每过一段时间，平台向插件发送 [META_PACKET_ACTION_HEARTBEAT] 数据包，并报告自身状态以维持连接。
 * 同一个物理连接上只需要一个心跳包发送任务。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
interface MetaPacket : Packet {
    val action: String
    val data: Any?
    val subject: SubjectDescriptor?
}