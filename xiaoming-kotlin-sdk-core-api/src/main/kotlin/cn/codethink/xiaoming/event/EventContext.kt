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

@file:JvmName("EventContexts")

package cn.codethink.xiaoming.event

import cn.codethink.xiaoming.event.operation.EventOperation

/**
 * 事件环境，用于管理发布一次事件后的相关状态。例如分发状态、监听器修改记录、取消记录、中断记录等等。
 *
 * @param E 事件类型
 * @author Chuanwise
 */
interface EventContext<E : Event> {
    /**
     * 目前最新的事件对象。
     */
    val event: E

    /**
     * 事件的状态。
     */
    val state: EventState

    /**
     * 事件的操作记录。
     */
    val operations: List<EventOperation>
}

val EventContext<*>.isPublishing: Boolean
    get() = state == EventState.PUBLISHING

val EventContext<*>.isPublished: Boolean
    get() = state == EventState.PUBLISHED

val EventContext<*>.isInterrupted: Boolean
    get() = state == EventState.INTERRUPTED

val EventContext<*>.isPublishedOrInterrupted: Boolean
    get() = isPublished || isInterrupted
