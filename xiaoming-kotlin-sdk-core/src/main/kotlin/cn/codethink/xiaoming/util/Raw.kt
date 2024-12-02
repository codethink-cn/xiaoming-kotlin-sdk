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

@file:JvmName("Raws")

package cn.codethink.xiaoming.util

import com.fasterxml.jackson.core.type.TypeReference

inline operator fun <reified T : Any?> Raw.get(name: String): T = get(
    name, object : TypeReference<T>() {}.type,
    optional = defaultOptional<T>(), nullable = defaultNullable<T>()
) as T

fun Iterable<String>.asSetEquals(keys: Iterable<String>): Boolean {
    val set = toSet()
    return set == keys.toSet()
}