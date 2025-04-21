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

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.packet.SessionPacket
import cn.codethink.xiaoming.util.Received
import cn.codethink.xiaoming.util.NotStableForInheritance

/**
 * 连接被拒绝异常。
 *
 * 当 [side] 为 [SessionSide.CURRENT_SIDE] 时，异常会在 [SharedConnection.start] 抛出，
 * 表示连接被对方拒绝。
 *
 * 当 [side] 为 [SessionSide.OTHER_SIDE] 时，异常可能在两处抛出：
 *
 * 1. 在 [SharedConnection.start] 中，表示己方向对方建立连接，连接被对方拒绝。
 * 2. 在 [SessionPassiveHandshakeContext.accept] 中，表示对方向己方建立连接，己方同意，但对方拒绝。
 *
 * @author Chuanwise
 * @see SessionHandshakeContext
 */
@NotStableForInheritance
abstract class SessionRejectedException(message: String) : SessionClosedException(message) {
    abstract val side: SessionSide
    abstract val packet: SessionPacket
    abstract val received: Received<SessionPacket>?
}