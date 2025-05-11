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

@file:JvmName("SegmentIds")

package cn.codethink.xiaoming.util

/**
 * 表示由一个或多个字符串片段组成的、通过 `.` 分割的 ID。
 *
 * 每个片段都是由英文字母、数字、下划线或减号组成的非空字符串。
 *
 * @author Chuanwise
 */
interface SegmentId : TextualId, List<String> {
    companion object {
        const val SEPARATOR = "."

        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String): SegmentId = string.toSegmentId()

        @JvmStatic
        @JavaFriendlyApi
        fun of(segments: List<String>): SegmentId = segments.toSegmentId()

        @JvmStatic
        @JavaFriendlyApi
        fun of(segment: String): SegmentId = segment.toSingleSegmentId()
    }

    fun toList(): List<String>
}

fun String.toSegmentId(): SegmentId = parseSegmentId(this)
fun String.toSingleSegmentId(): SegmentId = listOf(this).toSegmentId()
fun List<String>.toSegmentId(): SegmentId = createSegmentId(this)
