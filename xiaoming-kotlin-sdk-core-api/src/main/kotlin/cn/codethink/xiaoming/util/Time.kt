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

@file:JvmName("Times")
@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.api.CoreApi
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * 表示一个具体的时间。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
interface Time {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun now(): Time = nowTime

        @JvmStatic
        @JavaFriendlyApi
        fun ofMilliseconds(milliseconds: Long): Time = milliseconds.toMillisecondsTime()

        @JvmStatic
        @JavaFriendlyApi
        fun ofSeconds(seconds: Long): Time = seconds.toSecondsTime()
    }

    fun toMilliseconds(): Long
}

val nowTime: Time
    get() = currentTimeMillis.toMillisecondsTime()

fun Time.toSeconds(): Long = TimeUnit.MILLISECONDS.toSeconds(toMilliseconds())
fun Time.toDate(): Date = Date(toMilliseconds())
fun Time.format(format: DateFormat): String = format.format(toMilliseconds())

fun Long.toMillisecondsTime(): Time = CoreApi.getInstance().createTimeOfMilliseconds(this)
fun Long.toSecondsTime(): Time = TimeUnit.SECONDS.toMillis(this).toMillisecondsTime()