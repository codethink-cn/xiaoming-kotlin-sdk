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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 抽象事件。
 *
 * @property cause 事件的原因
 * @author Chuanwise
 * @see Event
 */
abstract class AbstractEvent @JvmOverloads constructor(
    override val cause: Cause? = null,
    initialIntercepted: Boolean = false
) : Event {
    @Transient
    private val mutableIntercepted = AtomicBoolean(initialIntercepted)

    override val isIntercepted: Boolean get() = mutableIntercepted.get()

    override fun intercept() {
        check(ensureIntercepted()) { "Event $this is already intercepted" }
    }

    override fun ensureIntercepted(): Boolean {
        return mutableIntercepted.compareAndSet(false, true)
    }
}