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

abstract class AbstractCancellableEvent @JvmOverloads constructor(
    override val cause: Cause? = null,
    initialCancelled: Boolean = false,
    initialIntercepted: Boolean = false
) : AbstractEvent(cause, initialIntercepted), CancellableEvent {
    @Transient
    private val mutableCancelled = AtomicBoolean(initialCancelled)

    override val isCancelled: Boolean get() = mutableCancelled.get()

    override fun cancel() {
        check(ensureCancelled()) { "Event $this is already cancelled" }
    }

    override fun ensureCancelled(): Boolean {
        return mutableCancelled.compareAndSet(false, true)
    }
}