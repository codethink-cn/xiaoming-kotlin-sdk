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

@file:JvmName("Defaults")

package cn.codethink.xiaoming.util

import java.util.function.Supplier
import kotlin.reflect.typeOf

inline fun <reified T> defaultNullable(): Boolean = typeOf<T>().isMarkedNullable
inline fun <reified T> defaultOptional(): Boolean = defaultNullable<T>()

private val nullValueSupplier = Supplier<Any?> { null }

@Suppress("UNCHECKED_CAST")
fun <T> nullValueFactory(): Supplier<T> = nullValueSupplier as Supplier<T>

inline fun <reified T> emptyValueFactory(): Supplier<T> = Supplier {
    if (T::class.java.isArray) {
        java.lang.reflect.Array.newInstance(T::class.java.componentType, 0) as T
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    when (T::class) {
        String::class -> ""
        Int::class -> 0
        Long::class -> 0L
        Short::class -> 0.toShort()
        Byte::class -> 0.toByte()
        Double::class -> 0.0
        Float::class -> 0.0f
        Char::class -> 0.toChar()
        Boolean::class -> false
        Map::class -> emptyMap<Nothing, Nothing>()
        List::class -> emptyList<Nothing>()
        Set::class -> emptySet<Nothing>()
        else -> getOrConstruct(T::class.java)
    } as T
}

inline fun <reified T> defaultValueFactory(): Supplier<T> = if (defaultNullable<T>()) {
    nullValueFactory()
} else {
    emptyValueFactory()
}