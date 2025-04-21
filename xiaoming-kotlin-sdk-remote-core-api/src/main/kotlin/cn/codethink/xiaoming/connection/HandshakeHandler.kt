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

import cn.codethink.xiaoming.util.TypeMeta
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 在一个可以复用的连接上收到连接数据包时，表示对方希望建立一个会话。
 * 会话的开启和关闭由 [HandshakeHandler] 处理。
 *
 * @author Chuanwise
 * @see SessionHandshakeContext
 */
interface HandshakeHandler<F, T> {
    val otherSideDataType: TypeMeta<T>

    @JvmBlockingBridge
    suspend fun onPassiveStart(context: SessionPassiveHandshakeContext<F>)

    @JvmBlockingBridge
    suspend fun onActiveStart(context: SessionActiveStartContext<T>)
}