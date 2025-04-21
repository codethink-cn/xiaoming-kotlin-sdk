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

@file:JvmName("ThreadLocals")

package cn.codethink.xiaoming.util

inline fun <T, R> ThreadLocal<T?>.withValue(value: T, block: () -> R): R {
    set(value)
    try {
        return block()
    } finally {
        remove()
    }
}

inline fun <R> ThreadLocal<Unit?>.withValue(block: () -> R): R = withValue(Unit, block)

inline fun <T, R> ThreadLocal<T?>.runIfEmpty(value: T, block: () -> R): R? {
    return if (get() == null) {
        withValue(value, block)
    } else {
        null
    }
}

inline fun <R> ThreadLocal<Unit?>.runIfEmpty(block: () -> R): R? = runIfEmpty(Unit, block)

fun <T> ThreadLocal<T?>.getOrFail(): T {
    return get() ?: throw NoSuchElementException("ThreadLocal value is null")
}