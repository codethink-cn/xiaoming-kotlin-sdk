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

import cn.codethink.xiaoming.util.JavaFriendlyApi
import cn.codethink.xiaoming.util.NotStableForInheritance

/**
 * 事件发布策略。
 *
 * @author Chuanwise
 */
@NotStableForInheritance
interface EventPublishPolicy {
    companion object {
        @JvmStatic
        val MUTABLE_INTERCEPTABLE = EventPublishPolicy(mutable = true, interceptable = true)

        @JvmStatic
        val IMMUTABLE_INTERCEPTABLE = EventPublishPolicy(mutable = false, interceptable = true)

        @JvmStatic
        val MUTABLE_NON_INTERCEPTABLE = EventPublishPolicy(mutable = true, interceptable = false)

        @JvmStatic
        val IMMUTABLE_NON_INTERCEPTABLE = EventPublishPolicy(mutable = false, interceptable = false)

        @JvmStatic
        @JavaFriendlyApi
        fun of(mutable: Boolean, interceptable: Boolean): EventPublishPolicy = EventPublishPolicy(mutable, interceptable)
    }

    val mutable: Boolean
    val interceptable: Boolean
}