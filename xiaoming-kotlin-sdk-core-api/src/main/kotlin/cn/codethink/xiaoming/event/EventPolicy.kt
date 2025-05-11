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

import cn.codethink.xiaoming.event.listener.ListenerIntent
import cn.codethink.xiaoming.util.NotStableForInheritance

/**
 * 事件发布策略。
 *
 * @author Chuanwise
 */
@NotStableForInheritance
interface EventPolicy {
    companion object {
        @JvmStatic
        val DEFAULT = EventPolicy(mutable = false, sticky = false)
    }

    /**
     * 事件是否可被修改。
     *
     * @see ListenerIntent
     */
    val mutable: Boolean

    /**
     * 事件是否粘性发布。
     */
    val sticky: Boolean
}