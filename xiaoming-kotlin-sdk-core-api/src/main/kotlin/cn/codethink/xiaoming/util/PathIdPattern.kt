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
 * 段 ID 匹配器：用于匹配由 `.` 分割的段 ID。
 *
 * 可被序列化为字符串，并被反序列化构造。
 *
 * @author Chuanwise
 * @see SegmentIdPatternElement
 */
@NotStableForInheritance
interface SegmentIdPattern {
    /**
     * 匹配器元素列表。
     */
    val elements: List<SegmentIdPatternElement>

    /**
     * 是否匹配指定的段 ID。
     *
     * @param id 段 ID
     * @return 是否匹配
     */
    fun matches(id: SegmentId): Boolean
}