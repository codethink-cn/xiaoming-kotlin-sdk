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

@file:JvmName("NumericalIds")
@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.util

/**
 * 数字化的 Id，可以转换为数字，并进行大小比较。
 *
 * @author Chuanwise
 */
interface NumericalId : Id, Comparable<NumericalId> {
    fun toInt(): Int
    fun toLong(): Long
}

fun Int.toNumericalId(): NumericalId = createNumericalId(this)
fun Long.toNumericalId(): NumericalId = createNumericalId(this)

