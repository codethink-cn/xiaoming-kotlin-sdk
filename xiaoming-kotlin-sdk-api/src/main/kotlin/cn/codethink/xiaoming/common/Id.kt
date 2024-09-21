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

@file:JvmName("Ids")

package cn.codethink.xiaoming.common

/**
 * Mark a class's instance as an id. All its implementation classes must be
 * serializable and deserializable, and implements the [equals], [toString]
 * and [hashCode].
 *
 * @author Chuanwise
 * @see NumericalId
 */
interface Id {
    override fun toString(): String
}


/**
 * A numerical id that can be converted to [Int] and [Long].
 *
 * @author Chuanwise
 * @see LongId
 */
interface NumericalId : Id {
    fun toInt(): Int
    fun toLong(): Long
}


@JvmInline
value class LongId(
    private val value: Long
) : Id, Comparable<LongId>, NumericalId {
    override fun compareTo(other: LongId): Int {
        return value.compareTo(other.value)
    }

    override fun toInt(): Int = value.toInt()

    override fun toLong(): Long = value

    override fun toString(): String {
        return value.toString()
    }
}

fun Int.toId(): NumericalId = LongId(this.toLong())
fun Long.toId(): NumericalId = LongId(this)


@JvmInline
value class StringId(
    private val value: String
) : Id, Comparable<StringId> {
    override fun compareTo(other: StringId): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return value
    }
}

fun String.toId(): StringId = StringId(this)