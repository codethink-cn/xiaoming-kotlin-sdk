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

package cn.codethink.xiaoming.event

import cn.codethink.xiaoming.Platform
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 事件管理器。
 *
 * @author Chuanwise
 */
interface EventManager {
    /**
     * 发布全局事件，事件将会被传播到本地和远程的监听器。事件必须可被序列化和反序列化。
     *
     * @param E 事件类型
     * @param event 事件
     * @param policy 事件发布策略
     * @return 事件上下文
     */
    @JvmBlockingBridge
    suspend fun <E : Event> publishEvent(event: E, policy: EventPolicy = EventPolicy.DEFAULT): EventContext<E>

    /**
     * 事件管理器对应的平台。
     */
    val platform: Platform
}