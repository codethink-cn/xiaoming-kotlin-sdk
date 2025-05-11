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

/**
 * 可取消事件。
 *
 * @author Chuanwise
 */
interface CancellableEvent : Event {
    val isCancelled: Boolean

    /**
     * 取消事件。
     *
     * @throws IllegalStateException 事件本已被取消
     */
    fun cancel()

    /**
     * 确保事件被取消。
     *
     * @return 事件是否因本次操作被取消
     */
    fun ensureCancelled(): Boolean
}