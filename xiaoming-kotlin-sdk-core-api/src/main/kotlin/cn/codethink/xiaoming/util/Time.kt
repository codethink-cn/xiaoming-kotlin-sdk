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

package cn.codethink.xiaoming.util

/**
 * 表示一个具体的时间。
 *
 * @author Chuanwise
 */
@NotStableForInheritance
interface Time : Comparable<Time> {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun now(): Time = nowTime

        @JvmStatic
        @JavaFriendlyApi
        fun ofUnixMilliseconds(milliseconds: Long): Time = createUnixMillisecondsTime(milliseconds)

        @JvmStatic
        @JavaFriendlyApi
        fun ofUnixSeconds(seconds: Long): Time = createUnixSecondsTime(seconds)
    }

    fun toUnixMilliseconds(): Long

    fun toUnixSeconds(): Long
}
