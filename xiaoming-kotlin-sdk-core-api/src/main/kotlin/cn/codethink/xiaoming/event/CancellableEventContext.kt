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

import cn.codethink.xiaoming.event.operation.SetCancelledEventOperation
import cn.codethink.xiaoming.util.Operation

interface CancellableEventContext<E : CancellableEvent> : EventContext<E> {
    val isCancelled: Boolean

    /**
     * 设置事件的取消状态。
     *
     * @param cancelled 新的取消状态
     * @param operation 取消的跟踪信息
     * @see SetCancelledEventOperation
     */
    fun setCancelled(cancelled: Boolean, operation: Operation)
}
