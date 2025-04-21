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
 * ## 事件发布
 *
 * 事件在发布时，有几个主要的设置：是否可变、发布范围（全局或本地）和是否可拦截。
 * 对于实现了 [CancellableEvent] 的事件，其还有是否可取消的设置。
 *
 * ### 事件可变性 - mutable
 *
 * 事件可变是指事件的字段可以被监听器修改，通常发布者需要等待所有事件监听器以允许监听器
 * 对事件字段的修改影响事件的结果。然而这会降低事件发布效率，因此默认事件按照只读方式发布。
 *
 * ### 事件发布范围 - global / local
 *
 * 事件只允许本地监听器处理，还是也允许远程监听器处理。默认发布范围是 global，此时事件也会
 * 被传递给其他进程内的监听器处理。
 *
 * ### 事件可拦截性 - cancellable
 *
 * 对于实现了 [CancellableEvent] 的事件，其可以通过可取消的方式发布，也可以通过不可取消
 * 的方式发布，默认通过可取消的方式发布。事件若被监听器取消，取消后事件将不再传递给其他监听器，
 * 除非监听器明确表示仍然需要监听已经取消的事件。事件被取消后也可以恢复。
 *
 * 事件的取消往往意味着对应操作的取消。
 *
 * ### 事件拦截 - intercept
 *
 * 事件若通过可被拦截的方式发布，则当监听器调用 [EventContext.intercept] 方法时，事件将
 * 不再传递给后续监听器。
 *
 * @author Chuanwise
 */
interface EventManager {
    /**
     * 发布全局事件，事件将会被传播到本地和远程的监听器。事件必须可被序列化和反序列化。
     *
     * @param E 事件类型
     * @param event 事件
     * @param policy 事件发布策略，默认为 [EventPublishPolicy.MUTABLE_INTERCEPTABLE]
     * @return 事件上下文
     */
    @JvmBlockingBridge
    suspend fun <E : Event> publishEvent(
        event: E,
        policy: EventPublishPolicy = EventPublishPolicy.MUTABLE_INTERCEPTABLE
    ): EventContext<E>

    @JvmBlockingBridge
    suspend fun <E : CancellableEvent> publishCancellableEvent(
        event: E,
        policy: EventPublishPolicy = EventPublishPolicy.MUTABLE_INTERCEPTABLE
    ): CancellableEventContext<E>

    /**
     * 事件管理器对应的平台。
     */
    val platform: Platform
}