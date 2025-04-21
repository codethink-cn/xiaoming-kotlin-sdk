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

@file:JvmName("Maps")

package cn.codethink.xiaoming.util

/**
 * 原子地比较 [Map] 中的值，并在它与值引用相同的时候删除它，否则不做任何变动。
 *
 * @param K 键类型
 * @param V 值类型
 * @param key 键
 * @param expect 预期值
 * @return 是否成功删除。
 */
@InternalApi
fun <K, V> MutableMap<K, V>.compareAndRemove(key: K, expect: V): Boolean {
    var result = false
    computeIfPresent(key) { _, actual ->
        if (actual === expect) {
            result = true
            null
        } else {
            result = false
            actual
        }
    }
    return result
}

/**
 * 原子地比较 [Map] 中的值，并在此前没有值，或 [replace] 为 `true` 时替换它。
 *
 * @param K 键类型
 * @param V 值类型
 * @param key 键
 * @param newValue 新值
 * @param replace 是否在存在老值时替换
 * @return 操作后该键对应的值引用。若成功替换，返回新值引用，否则返回当前对应于该键的值引用。
 */
@InternalApi
fun <K, V> MutableMap<K, V>.computeIfReplace(key: K, newValue: V, replace: Boolean): V? {
    return compute(key) { _, oldValue ->
        if (oldValue === null || replace) {
            newValue
        } else {
            oldValue
        }
    }
}
