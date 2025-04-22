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

package cn.codethink.xiaoming.util

/**
 * 通配符路径 ID 匹配器元素，可以匹配 0 个或多个路径 ID 的部分。
 *
 * @property isOptional 是否可以匹配 0 个部分
 * @property isGreedy 是否贪婪匹配
 */
enum class WildCardSegmentIdPatternElement(
    val isOptional: Boolean,
    val isGreedy: Boolean
) : SegmentIdPatternElement {
    OPTIONAL(true, false),
    REQUIRED(false, false),

    GREEDY_OPTIONAL(true, true),
    GREEDY_REQUIRED(false, true);

    override fun toString(): String = when (this) {
        OPTIONAL -> "?"
        REQUIRED -> "+"
        GREEDY_OPTIONAL -> "??"
        GREEDY_REQUIRED -> "++"
    }
}