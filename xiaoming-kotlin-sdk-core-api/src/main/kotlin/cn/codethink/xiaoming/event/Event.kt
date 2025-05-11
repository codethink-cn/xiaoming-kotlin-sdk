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

import cn.codethink.xiaoming.util.Cause

/**
 * 事件接口。其实现类必须继承 [AbstractEvent]。
 *
 * @author Chuanwise
 * @see LocalEvent
 * @see EventContext
 * @see CancellableEvent
 */
interface Event : Cause {
    /**
     * 事件是否被拦截。
     */
    val isIntercepted: Boolean

    /**
     * 拦截事件。
     *
     * @throws IllegalStateException 事件本已被拦截
     */
    fun intercept()

    /**
     * 确保事件被拦截。
     *
     * @return 事件是否因本次操作被拦截
     */
    fun ensureIntercepted(): Boolean
}