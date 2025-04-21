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

package cn.codethink.xiaoming.event

import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.event.operation.EventOperation
import cn.codethink.xiaoming.util.Cause

/**
 * 事件环境，用于管理发布一次事件后的相关状态。例如分发状态、监听器修改记录、取消记录、拦截记录等等。
 *
 * @param E 事件类型
 * @author Chuanwise
 */
interface EventContext<out E : Event> {
    /**
     * 事件发布的平台。
     */
    val platform: Platform

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

    /**
     * 事件是否可被拦截。
     */
    val isInterceptable: Boolean

    /**
     * 事件是否已经被拦截。
     */
    val isIntercepted: Boolean

    /**
     * 尝试拦截事件
     *
     * @param cause 拦截的原因
     * @return 是否成功拦截
     */
    fun tryIntercept(cause: Cause): Boolean

    /**
     * 拦截事件。
     *
     * @param cause 拦截的原因
     * @throws UnsupportedOperationException 事件不支持被拦截
     * @throws IllegalStateException 事件已经被拦截，或已经被发布
     */
    fun intercept(cause: Cause)

    /**
     * 拦截事件，若无法拦截则忽略。
     *
     * @param cause 拦截的原因
     */
    fun ensureIntercepted(cause: Cause): Boolean {
        tryIntercept(cause)
        return isIntercepted
    }
}