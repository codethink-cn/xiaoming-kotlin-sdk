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

@file:JvmName("Causes")

package cn.codethink.xiaoming.util

/**
 * 表示一件事或一个操作的原因。
 *
 * @author Chuanwise
 * @see createCause
 */
interface Cause {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun of(text: String, subject: SubjectDescriptor): Cause = createCause(text, subject)
    }

    /**
     * 原因类型。
     */
    val type: String

    /**
     * 提供这个原因的主体。
     */
    val subject: SubjectDescriptor

    /**
     * 直接原因。
     */
    val cause: Cause?

    /**
     * 描述原因的一段话，一般使用英文。
     */
    val message: String
}

/**
 * 获取根原因。
 */
val Cause.rootCause: Cause
    get() {
        var result = this
        while (true) {
            result = result.cause ?: return result
            require(result !== this) { "Cause has a cycle: $this." }
        }
    }