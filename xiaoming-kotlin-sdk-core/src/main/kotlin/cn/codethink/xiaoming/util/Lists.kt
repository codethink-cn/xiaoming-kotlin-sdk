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

@file:JvmName("Lists")

package cn.codethink.xiaoming.util

@InternalApi
fun <T> List<List<T>>.products(): Sequence<List<T>> {
    // 若有任意一个列表为空，则结果一定为空
    if (any { it.isEmpty() }) return emptySequence()

    return sequence {
        val indices = IntArray(size) { 0 }
        while (true) {
            // 根据当前 indices 构造一个组合
            val elements = List(size) { i -> this@products[i][indices[i]] }
            yield(elements)

            // 更新 indices 类似进位逻辑
            var i = size - 1
            while (i >= 0) {
                indices[i]++
                if (indices[i] < this@products[i].size) {
                    break
                } else {
                    indices[i] = 0
                    i--
                }
            }
            if (i < 0) break // 全部组合已生成
        }
    }
}

@InternalApi
fun <T> List<List<T>>.productSize(): Int {
    if (isEmpty()) {
        return 0
    }
    var result = 1
    for (list in this) {
        result *= list.size
        if (result == 0) {
            return 0
        }
    }
    return result
}