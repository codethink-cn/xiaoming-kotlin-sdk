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
@file:Suppress("UNCHECKED_CAST")

package cn.codethink.xiaoming.util

import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

fun <T> Raw.get(name: String, type: RawFieldType<T>) = get(
    name = name,
    type = type.type,
    optional = type.optional,
    nullable = type.nullable,
    convertable = type.convertable,
    defaultValueFactory = type.defaultValueFactory
) as T

// Char

@JvmOverloads
fun Raw.getChar(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, Char::class.java, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Char

fun Raw.getCharOrDefault(name: String, defaultValue: Char) =
    get(name, Char::class.java, optional = true, nullable = false) { defaultValue } as Char

fun Raw.getCharOrFail(name: String) = get(name, Char::class.java, optional = false, nullable = false) as Char
fun Raw.getCharOrNull(name: String) = get(name, Char::class.java, optional = true, nullable = true) as Char?

@JvmOverloads
fun Raw.getAsChar(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Char::class.java,
    optional = false,
    nullable = false,
    convertable = true,
    defaultValueFactory = defaultValueFactory
) as Char

fun Raw.getAsCharOrDefault(name: String, defaultValue: Char) =
    get(name, Char::class.java, optional = true, nullable = false, convertable = true) { defaultValue } as Char

fun Raw.getAsCharOrFail(name: String) =
    get(name, Char::class.java, optional = false, nullable = false, convertable = true) as Char

fun Raw.getAsCharOrNull(name: String) =
    get(name, Char::class.java, optional = true, nullable = true, convertable = true) as Char?

private val charListType = typeOf<List<Char>>().javaType

@JvmOverloads
fun Raw.getCharList(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, charListType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as List<Char>

fun Raw.getCharListOrDefault(name: String, defaultValue: List<Char>) =
    get(name, charListType, optional = true, nullable = false) { defaultValue } as List<Char>

fun Raw.getCharListOrFail(name: String) = get(name, charListType, optional = false, nullable = false) as List<Char>
fun Raw.getCharListOrNull(name: String) = get(name, charListType, optional = true, nullable = true) as List<Char>?

private val charSetType = typeOf<Set<Char>>().javaType

@JvmOverloads
fun Raw.getCharSet(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, charSetType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Set<Char>

fun Raw.getCharSetOrDefault(name: String, defaultValue: Set<Char>) =
    get(name, charSetType, optional = true, nullable = false) { defaultValue } as Set<Char>

fun Raw.getCharSetOrFail(name: String) = get(name, charSetType, optional = false, nullable = false) as Set<Char>
fun Raw.getCharSetOrNull(name: String) = get(name, charSetType, optional = true, nullable = true) as Set<Char>?

private val charCollectionType = typeOf<Collection<Char>>().javaType

@JvmOverloads
fun Raw.getCharCollection(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    charCollectionType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Collection<Char>

fun Raw.getCharCollectionOrDefault(name: String, defaultValue: Collection<Char>) =
    get(name, charCollectionType, optional = true, nullable = false) { defaultValue } as Collection<Char>

fun Raw.getCharCollectionOrFail(name: String) =
    get(name, charCollectionType, optional = false, nullable = false) as Collection<Char>

fun Raw.getCharCollectionOrNull(name: String) =
    get(name, charCollectionType, optional = true, nullable = true) as Collection<Char>?

private val charIterableType = typeOf<Iterable<Char>>().javaType

@JvmOverloads
fun Raw.getCharIterable(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    charIterableType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Iterable<Char>

fun Raw.getCharIterableOrDefault(name: String, defaultValue: Iterable<Char>) =
    get(name, charIterableType, optional = true, nullable = false) { defaultValue } as Iterable<Char>

fun Raw.getCharIterableOrFail(name: String) =
    get(name, charIterableType, optional = false, nullable = false) as Iterable<Char>

fun Raw.getCharIterableOrNull(name: String) =
    get(name, charIterableType, optional = true, nullable = true) as Iterable<Char>?

// Byte

@JvmOverloads
fun Raw.getByte(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, Byte::class.java, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Byte

fun Raw.getByteOrDefault(name: String, defaultValue: Byte) =
    get(name, Byte::class.java, optional = true, nullable = false) { defaultValue } as Byte

fun Raw.getByteOrFail(name: String) = get(name, Byte::class.java, optional = false, nullable = false) as Byte
fun Raw.getByteOrNull(name: String) = get(name, Byte::class.java, optional = true, nullable = true) as Byte?

@JvmOverloads
fun Raw.getAsByte(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Byte::class.java,
    optional = false,
    nullable = false,
    convertable = true,
    defaultValueFactory = defaultValueFactory
) as Byte

fun Raw.getAsByteOrDefault(name: String, defaultValue: Byte) =
    get(name, Byte::class.java, optional = true, nullable = false, convertable = true) { defaultValue } as Byte

fun Raw.getAsByteOrFail(name: String) =
    get(name, Byte::class.java, optional = false, nullable = false, convertable = true) as Byte

fun Raw.getAsByteOrNull(name: String) =
    get(name, Byte::class.java, optional = true, nullable = true, convertable = true) as Byte?

private val byteListType = typeOf<List<Byte>>().javaType

@JvmOverloads
fun Raw.getByteList(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, byteListType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as List<Byte>

fun Raw.getByteListOrDefault(name: String, defaultValue: List<Byte>) =
    get(name, byteListType, optional = true, nullable = false) { defaultValue } as List<Byte>

fun Raw.getByteListOrFail(name: String) = get(name, byteListType, optional = false, nullable = false) as List<Byte>
fun Raw.getByteListOrNull(name: String) = get(name, byteListType, optional = true, nullable = true) as List<Byte>?

private val byteSetType = typeOf<Set<Byte>>().javaType

@JvmOverloads
fun Raw.getByteSet(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, byteSetType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Set<Byte>

fun Raw.getByteSetOrDefault(name: String, defaultValue: Set<Byte>) =
    get(name, byteSetType, optional = true, nullable = false) { defaultValue } as Set<Byte>

fun Raw.getByteSetOrFail(name: String) = get(name, byteSetType, optional = false, nullable = false) as Set<Byte>
fun Raw.getByteSetOrNull(name: String) = get(name, byteSetType, optional = true, nullable = true) as Set<Byte>?

private val byteCollectionType = typeOf<Collection<Byte>>().javaType

@JvmOverloads
fun Raw.getByteCollection(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    byteCollectionType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Collection<Byte>

fun Raw.getByteCollectionOrDefault(name: String, defaultValue: Collection<Byte>) =
    get(name, byteCollectionType, optional = true, nullable = false) { defaultValue } as Collection<Byte>

fun Raw.getByteCollectionOrFail(name: String) =
    get(name, byteCollectionType, optional = false, nullable = false) as Collection<Byte>

fun Raw.getByteCollectionOrNull(name: String) =
    get(name, byteCollectionType, optional = true, nullable = true) as Collection<Byte>?

private val byteIterableType = typeOf<Iterable<Byte>>().javaType

@JvmOverloads
fun Raw.getByteIterable(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    byteIterableType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Iterable<Byte>

fun Raw.getByteIterableOrDefault(name: String, defaultValue: Iterable<Byte>) =
    get(name, byteIterableType, optional = true, nullable = false) { defaultValue } as Iterable<Byte>

fun Raw.getByteIterableOrFail(name: String) =
    get(name, byteIterableType, optional = false, nullable = false) as Iterable<Byte>

fun Raw.getByteIterableOrNull(name: String) =
    get(name, byteIterableType, optional = true, nullable = true) as Iterable<Byte>?

// Boolean

@JvmOverloads
fun Raw.getBoolean(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Boolean::class.java,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Boolean

fun Raw.getBooleanOrDefault(name: String, defaultValue: Boolean) =
    get(name, Boolean::class.java, optional = true, nullable = false) { defaultValue } as Boolean

fun Raw.getBooleanOrFail(name: String) = get(name, Boolean::class.java, optional = false, nullable = false) as Boolean
fun Raw.getBooleanOrNull(name: String) = get(name, Boolean::class.java, optional = true, nullable = true) as Boolean?

@JvmOverloads
fun Raw.getAsBoolean(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Boolean::class.java,
    optional = false,
    nullable = false,
    convertable = true,
    defaultValueFactory = defaultValueFactory
) as Boolean

fun Raw.getAsBooleanOrDefault(name: String, defaultValue: Boolean) =
    get(name, Boolean::class.java, optional = true, nullable = false, convertable = true) { defaultValue } as Boolean

fun Raw.getAsBooleanOrFail(name: String) =
    get(name, Boolean::class.java, optional = false, nullable = false, convertable = true) as Boolean

fun Raw.getAsBooleanOrNull(name: String) =
    get(name, Boolean::class.java, optional = true, nullable = true, convertable = true) as Boolean?

private val booleanListType = typeOf<List<Boolean>>().javaType

@JvmOverloads
fun Raw.getBooleanList(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    booleanListType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as List<Boolean>

fun Raw.getBooleanListOrDefault(name: String, defaultValue: List<Boolean>) =
    get(name, booleanListType, optional = true, nullable = false) { defaultValue } as List<Boolean>

fun Raw.getBooleanListOrFail(name: String) =
    get(name, booleanListType, optional = false, nullable = false) as List<Boolean>

fun Raw.getBooleanListOrNull(name: String) =
    get(name, booleanListType, optional = true, nullable = true) as List<Boolean>?

private val booleanSetType = typeOf<Set<Boolean>>().javaType

@JvmOverloads
fun Raw.getBooleanSet(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    booleanSetType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Set<Boolean>

fun Raw.getBooleanSetOrDefault(name: String, defaultValue: Set<Boolean>) =
    get(name, booleanSetType, optional = true, nullable = false) { defaultValue } as Set<Boolean>

fun Raw.getBooleanSetOrFail(name: String) =
    get(name, booleanSetType, optional = false, nullable = false) as Set<Boolean>

fun Raw.getBooleanSetOrNull(name: String) = get(name, booleanSetType, optional = true, nullable = true) as Set<Boolean>?

private val booleanCollectionType = typeOf<Collection<Boolean>>().javaType

@JvmOverloads
fun Raw.getBooleanCollection(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    booleanCollectionType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Collection<Boolean>

fun Raw.getBooleanCollectionOrDefault(name: String, defaultValue: Collection<Boolean>) =
    get(name, booleanCollectionType, optional = true, nullable = false) { defaultValue } as Collection<Boolean>

fun Raw.getBooleanCollectionOrFail(name: String) =
    get(name, booleanCollectionType, optional = false, nullable = false) as Collection<Boolean>

fun Raw.getBooleanCollectionOrNull(name: String) =
    get(name, booleanCollectionType, optional = true, nullable = true) as Collection<Boolean>?

private val booleanIterableType = typeOf<Iterable<Boolean>>().javaType

@JvmOverloads
fun Raw.getBooleanIterable(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    booleanIterableType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Iterable<Boolean>

fun Raw.getBooleanIterableOrDefault(name: String, defaultValue: Iterable<Boolean>) =
    get(name, booleanIterableType, optional = true, nullable = false) { defaultValue } as Iterable<Boolean>

fun Raw.getBooleanIterableOrFail(name: String) =
    get(name, booleanIterableType, optional = false, nullable = false) as Iterable<Boolean>

fun Raw.getBooleanIterableOrNull(name: String) =
    get(name, booleanIterableType, optional = true, nullable = true) as Iterable<Boolean>?

// String

@JvmOverloads
fun Raw.getString(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    String::class.java,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as String

fun Raw.getStringOrDefault(name: String, defaultValue: String) =
    get(name, String::class.java, optional = true, nullable = false) { defaultValue } as String

fun Raw.getStringOrFail(name: String) = get(name, String::class.java, optional = false, nullable = false) as String
fun Raw.getStringOrNull(name: String) = get(name, String::class.java, optional = true, nullable = true) as String?

@JvmOverloads
fun Raw.getAsString(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    String::class.java,
    optional = false,
    nullable = false,
    convertable = true,
    defaultValueFactory = defaultValueFactory
) as String

fun Raw.getAsStringOrDefault(name: String, defaultValue: String) =
    get(name, String::class.java, optional = true, nullable = false, convertable = true) { defaultValue } as String

fun Raw.getAsStringOrFail(name: String) =
    get(name, String::class.java, optional = false, nullable = false, convertable = true) as String

fun Raw.getAsStringOrNull(name: String) =
    get(name, String::class.java, optional = true, nullable = true, convertable = true) as String?

private val stringListType = typeOf<List<String>>().javaType

@JvmOverloads
fun Raw.getStringList(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    stringListType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as List<String>

fun Raw.getStringListOrDefault(name: String, defaultValue: List<String>) =
    get(name, stringListType, optional = true, nullable = false) { defaultValue } as List<String>

fun Raw.getStringListOrFail(name: String) =
    get(name, stringListType, optional = false, nullable = false) as List<String>

fun Raw.getStringListOrNull(name: String) = get(name, stringListType, optional = true, nullable = true) as List<String>?

private val stringSetType = typeOf<Set<String>>().javaType

@JvmOverloads
fun Raw.getStringSet(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    stringSetType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Set<String>

fun Raw.getStringSetOrDefault(name: String, defaultValue: Set<String>) =
    get(name, stringSetType, optional = true, nullable = false) { defaultValue } as Set<String>

fun Raw.getStringSetOrFail(name: String) = get(name, stringSetType, optional = false, nullable = false) as Set<String>
fun Raw.getStringSetOrNull(name: String) = get(name, stringSetType, optional = true, nullable = true) as Set<String>?

private val stringCollectionType = typeOf<Collection<String>>().javaType

@JvmOverloads
fun Raw.getStringCollection(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    stringCollectionType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Collection<String>

fun Raw.getStringCollectionOrDefault(name: String, defaultValue: Collection<String>) =
    get(name, stringCollectionType, optional = true, nullable = false) { defaultValue } as Collection<String>

fun Raw.getStringCollectionOrFail(name: String) =
    get(name, stringCollectionType, optional = false, nullable = false) as Collection<String>

fun Raw.getStringCollectionOrNull(name: String) =
    get(name, stringCollectionType, optional = true, nullable = true) as Collection<String>?

private val stringIterableType = typeOf<Iterable<String>>().javaType

@JvmOverloads
fun Raw.getStringIterable(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    stringIterableType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Iterable<String>

fun Raw.getStringIterableOrDefault(name: String, defaultValue: Iterable<String>) =
    get(name, stringIterableType, optional = true, nullable = false) { defaultValue } as Iterable<String>

fun Raw.getStringIterableOrFail(name: String) =
    get(name, stringIterableType, optional = false, nullable = false) as Iterable<String>

fun Raw.getStringIterableOrNull(name: String) =
    get(name, stringIterableType, optional = true, nullable = true) as Iterable<String>?

// Short

@JvmOverloads
fun Raw.getShort(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, Short::class.java, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Short

fun Raw.getShortOrDefault(name: String, defaultValue: Short) =
    get(name, Short::class.java, optional = true, nullable = false) { defaultValue } as Short

fun Raw.getShortOrFail(name: String) = get(name, Short::class.java, optional = false, nullable = false) as Short
fun Raw.getShortOrNull(name: String) = get(name, Short::class.java, optional = true, nullable = true) as Short?

@JvmOverloads
fun Raw.getAsShort(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Short::class.java,
    optional = false,
    nullable = false,
    convertable = true,
    defaultValueFactory = defaultValueFactory
) as Short

fun Raw.getAsShortOrDefault(name: String, defaultValue: Short) =
    get(name, Short::class.java, optional = true, nullable = false, convertable = true) { defaultValue } as Short

fun Raw.getAsShortOrFail(name: String) =
    get(name, Short::class.java, optional = false, nullable = false, convertable = true) as Short

fun Raw.getAsShortOrNull(name: String) =
    get(name, Short::class.java, optional = true, nullable = true, convertable = true) as Short?

private val shortListType = typeOf<List<Short>>().javaType

@JvmOverloads
fun Raw.getShortList(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    shortListType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as List<Short>

fun Raw.getShortListOrDefault(name: String, defaultValue: List<Short>) =
    get(name, shortListType, optional = true, nullable = false) { defaultValue } as List<Short>

fun Raw.getShortListOrFail(name: String) = get(name, shortListType, optional = false, nullable = false) as List<Short>
fun Raw.getShortListOrNull(name: String) = get(name, shortListType, optional = true, nullable = true) as List<Short>?

private val shortSetType = typeOf<Set<Short>>().javaType

@JvmOverloads
fun Raw.getShortSet(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, shortSetType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Set<Short>

fun Raw.getShortSetOrDefault(name: String, defaultValue: Set<Short>) =
    get(name, shortSetType, optional = true, nullable = false) { defaultValue } as Set<Short>

fun Raw.getShortSetOrFail(name: String) = get(name, shortSetType, optional = false, nullable = false) as Set<Short>
fun Raw.getShortSetOrNull(name: String) = get(name, shortSetType, optional = true, nullable = true) as Set<Short>?

private val shortCollectionType = typeOf<Collection<Short>>().javaType

@JvmOverloads
fun Raw.getShortCollection(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    shortCollectionType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Collection<Short>

fun Raw.getShortCollectionOrDefault(name: String, defaultValue: Collection<Short>) =
    get(name, shortCollectionType, optional = true, nullable = false) { defaultValue } as Collection<Short>

fun Raw.getShortCollectionOrFail(name: String) =
    get(name, shortCollectionType, optional = false, nullable = false) as Collection<Short>

fun Raw.getShortCollectionOrNull(name: String) =
    get(name, shortCollectionType, optional = true, nullable = true) as Collection<Short>?

private val shortIterableType = typeOf<Iterable<Short>>().javaType

@JvmOverloads
fun Raw.getShortIterable(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    shortIterableType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Iterable<Short>

fun Raw.getShortIterableOrDefault(name: String, defaultValue: Iterable<Short>) =
    get(name, shortIterableType, optional = true, nullable = false) { defaultValue } as Iterable<Short>

fun Raw.getShortIterableOrFail(name: String) =
    get(name, shortIterableType, optional = false, nullable = false) as Iterable<Short>

fun Raw.getShortIterableOrNull(name: String) =
    get(name, shortIterableType, optional = true, nullable = true) as Iterable<Short>?

// Int

@JvmOverloads
fun Raw.getInt(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, Int::class.java, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Int

fun Raw.getIntOrDefault(name: String, defaultValue: Int) =
    get(name, Int::class.java, optional = true, nullable = false) { defaultValue } as Int

fun Raw.getIntOrFail(name: String) = get(name, Int::class.java, optional = false, nullable = false) as Int
fun Raw.getIntOrNull(name: String) = get(name, Int::class.java, optional = true, nullable = true) as Int?

@JvmOverloads
fun Raw.getAsInt(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Int::class.java,
    optional = false,
    nullable = false,
    convertable = true,
    defaultValueFactory = defaultValueFactory
) as Int

fun Raw.getAsIntOrDefault(name: String, defaultValue: Int) =
    get(name, Int::class.java, optional = true, nullable = false, convertable = true) { defaultValue } as Int

fun Raw.getAsIntOrFail(name: String) =
    get(name, Int::class.java, optional = false, nullable = false, convertable = true) as Int

fun Raw.getAsIntOrNull(name: String) =
    get(name, Int::class.java, optional = true, nullable = true, convertable = true) as Int?

private val intListType = typeOf<List<Int>>().javaType

@JvmOverloads
fun Raw.getIntList(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, intListType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as List<Int>

fun Raw.getIntListOrDefault(name: String, defaultValue: List<Int>) =
    get(name, intListType, optional = true, nullable = false) { defaultValue } as List<Int>

fun Raw.getIntListOrFail(name: String) = get(name, intListType, optional = false, nullable = false) as List<Int>
fun Raw.getIntListOrNull(name: String) = get(name, intListType, optional = true, nullable = true) as List<Int>?

private val intSetType = typeOf<Set<Int>>().javaType

@JvmOverloads
fun Raw.getIntSet(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, intSetType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Set<Int>

fun Raw.getIntSetOrDefault(name: String, defaultValue: Set<Int>) =
    get(name, intSetType, optional = true, nullable = false) { defaultValue } as Set<Int>

fun Raw.getIntSetOrFail(name: String) = get(name, intSetType, optional = false, nullable = false) as Set<Int>
fun Raw.getIntSetOrNull(name: String) = get(name, intSetType, optional = true, nullable = true) as Set<Int>?

private val intCollectionType = typeOf<Collection<Int>>().javaType

@JvmOverloads
fun Raw.getIntCollection(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    intCollectionType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Collection<Int>

fun Raw.getIntCollectionOrDefault(name: String, defaultValue: Collection<Int>) =
    get(name, intCollectionType, optional = true, nullable = false) { defaultValue } as Collection<Int>

fun Raw.getIntCollectionOrFail(name: String) =
    get(name, intCollectionType, optional = false, nullable = false) as Collection<Int>

fun Raw.getIntCollectionOrNull(name: String) =
    get(name, intCollectionType, optional = true, nullable = true) as Collection<Int>?

private val intIterableType = typeOf<Iterable<Int>>().javaType

@JvmOverloads
fun Raw.getIntIterable(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    intIterableType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Iterable<Int>

fun Raw.getIntIterableOrDefault(name: String, defaultValue: Iterable<Int>) =
    get(name, intIterableType, optional = true, nullable = false) { defaultValue } as Iterable<Int>

fun Raw.getIntIterableOrFail(name: String) =
    get(name, intIterableType, optional = false, nullable = false) as Iterable<Int>

fun Raw.getIntIterableOrNull(name: String) =
    get(name, intIterableType, optional = true, nullable = true) as Iterable<Int>?

// Long

@JvmOverloads
fun Raw.getLong(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, Long::class.java, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Long

fun Raw.getLongOrDefault(name: String, defaultValue: Long) =
    get(name, Long::class.java, optional = true, nullable = false) { defaultValue } as Long

fun Raw.getLongOrFail(name: String) = get(name, Long::class.java, optional = false, nullable = false) as Long
fun Raw.getLongOrNull(name: String) = get(name, Long::class.java, optional = true, nullable = true) as Long?

@JvmOverloads
fun Raw.getAsLong(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Long::class.java,
    optional = false,
    nullable = false,
    convertable = true,
    defaultValueFactory = defaultValueFactory
) as Long

fun Raw.getAsLongOrDefault(name: String, defaultValue: Long) =
    get(name, Long::class.java, optional = true, nullable = false, convertable = true) { defaultValue } as Long

fun Raw.getAsLongOrFail(name: String) =
    get(name, Long::class.java, optional = false, nullable = false, convertable = true) as Long

fun Raw.getAsLongOrNull(name: String) =
    get(name, Long::class.java, optional = true, nullable = true, convertable = true) as Long?

private val longListType = typeOf<List<Long>>().javaType

@JvmOverloads
fun Raw.getLongList(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, longListType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as List<Long>

fun Raw.getLongListOrDefault(name: String, defaultValue: List<Long>) =
    get(name, longListType, optional = true, nullable = false) { defaultValue } as List<Long>

fun Raw.getLongListOrFail(name: String) = get(name, longListType, optional = false, nullable = false) as List<Long>
fun Raw.getLongListOrNull(name: String) = get(name, longListType, optional = true, nullable = true) as List<Long>?

private val longSetType = typeOf<Set<Long>>().javaType

@JvmOverloads
fun Raw.getLongSet(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, longSetType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Set<Long>

fun Raw.getLongSetOrDefault(name: String, defaultValue: Set<Long>) =
    get(name, longSetType, optional = true, nullable = false) { defaultValue } as Set<Long>

fun Raw.getLongSetOrFail(name: String) = get(name, longSetType, optional = false, nullable = false) as Set<Long>
fun Raw.getLongSetOrNull(name: String) = get(name, longSetType, optional = true, nullable = true) as Set<Long>?

private val longCollectionType = typeOf<Collection<Long>>().javaType

@JvmOverloads
fun Raw.getLongCollection(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    longCollectionType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Collection<Long>

fun Raw.getLongCollectionOrDefault(name: String, defaultValue: Collection<Long>) =
    get(name, longCollectionType, optional = true, nullable = false) { defaultValue } as Collection<Long>

fun Raw.getLongCollectionOrFail(name: String) =
    get(name, longCollectionType, optional = false, nullable = false) as Collection<Long>

fun Raw.getLongCollectionOrNull(name: String) =
    get(name, longCollectionType, optional = true, nullable = true) as Collection<Long>?

private val longIterableType = typeOf<Iterable<Long>>().javaType

@JvmOverloads
fun Raw.getLongIterable(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    longIterableType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Iterable<Long>

fun Raw.getLongIterableOrDefault(name: String, defaultValue: Iterable<Long>) =
    get(name, longIterableType, optional = true, nullable = false) { defaultValue } as Iterable<Long>

fun Raw.getLongIterableOrFail(name: String) =
    get(name, longIterableType, optional = false, nullable = false) as Iterable<Long>

fun Raw.getLongIterableOrNull(name: String) =
    get(name, longIterableType, optional = true, nullable = true) as Iterable<Long>?

// Float

@JvmOverloads
fun Raw.getFloat(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, Float::class.java, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Float

fun Raw.getFloatOrDefault(name: String, defaultValue: Float) =
    get(name, Float::class.java, optional = true, nullable = false) { defaultValue } as Float

fun Raw.getFloatOrFail(name: String) = get(name, Float::class.java, optional = false, nullable = false) as Float
fun Raw.getFloatOrNull(name: String) = get(name, Float::class.java, optional = true, nullable = true) as Float?

@JvmOverloads
fun Raw.getAsFloat(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Float::class.java,
    optional = false,
    nullable = false,
    convertable = true,
    defaultValueFactory = defaultValueFactory
) as Float

fun Raw.getAsFloatOrDefault(name: String, defaultValue: Float) =
    get(name, Float::class.java, optional = true, nullable = false, convertable = true) { defaultValue } as Float

fun Raw.getAsFloatOrFail(name: String) =
    get(name, Float::class.java, optional = false, nullable = false, convertable = true) as Float

fun Raw.getAsFloatOrNull(name: String) =
    get(name, Float::class.java, optional = true, nullable = true, convertable = true) as Float?

private val floatListType = typeOf<List<Float>>().javaType

@JvmOverloads
fun Raw.getFloatList(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    floatListType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as List<Float>

fun Raw.getFloatListOrDefault(name: String, defaultValue: List<Float>) =
    get(name, floatListType, optional = true, nullable = false) { defaultValue } as List<Float>

fun Raw.getFloatListOrFail(name: String) = get(name, floatListType, optional = false, nullable = false) as List<Float>
fun Raw.getFloatListOrNull(name: String) = get(name, floatListType, optional = true, nullable = true) as List<Float>?

private val floatSetType = typeOf<Set<Float>>().javaType

@JvmOverloads
fun Raw.getFloatSet(name: String, defaultValueFactory: (() -> Any?)? = null) =
    get(name, floatSetType, optional = false, nullable = false, defaultValueFactory = defaultValueFactory) as Set<Float>

fun Raw.getFloatSetOrDefault(name: String, defaultValue: Set<Float>) =
    get(name, floatSetType, optional = true, nullable = false) { defaultValue } as Set<Float>

fun Raw.getFloatSetOrFail(name: String) = get(name, floatSetType, optional = false, nullable = false) as Set<Float>
fun Raw.getFloatSetOrNull(name: String) = get(name, floatSetType, optional = true, nullable = true) as Set<Float>?

private val floatCollectionType = typeOf<Collection<Float>>().javaType

@JvmOverloads
fun Raw.getFloatCollection(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    floatCollectionType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Collection<Float>

fun Raw.getFloatCollectionOrDefault(name: String, defaultValue: Collection<Float>) =
    get(name, floatCollectionType, optional = true, nullable = false) { defaultValue } as Collection<Float>

fun Raw.getFloatCollectionOrFail(name: String) =
    get(name, floatCollectionType, optional = false, nullable = false) as Collection<Float>

fun Raw.getFloatCollectionOrNull(name: String) =
    get(name, floatCollectionType, optional = true, nullable = true) as Collection<Float>?

private val floatIterableType = typeOf<Iterable<Float>>().javaType

@JvmOverloads
fun Raw.getFloatIterable(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    floatIterableType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Iterable<Float>

fun Raw.getFloatIterableOrDefault(name: String, defaultValue: Iterable<Float>) =
    get(name, floatIterableType, optional = true, nullable = false) { defaultValue } as Iterable<Float>

fun Raw.getFloatIterableOrFail(name: String) =
    get(name, floatIterableType, optional = false, nullable = false) as Iterable<Float>

fun Raw.getFloatIterableOrNull(name: String) =
    get(name, floatIterableType, optional = true, nullable = true) as Iterable<Float>?

// Double

@JvmOverloads
fun Raw.getDouble(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Double::class.java,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Double

fun Raw.getDoubleOrDefault(name: String, defaultValue: Double) =
    get(name, Double::class.java, optional = true, nullable = false) { defaultValue } as Double

fun Raw.getDoubleOrFail(name: String) = get(name, Double::class.java, optional = false, nullable = false) as Double
fun Raw.getDoubleOrNull(name: String) = get(name, Double::class.java, optional = true, nullable = true) as Double?

@JvmOverloads
fun Raw.getAsDouble(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    Double::class.java,
    optional = false,
    nullable = false,
    convertable = true,
    defaultValueFactory = defaultValueFactory
) as Double

fun Raw.getAsDoubleOrDefault(name: String, defaultValue: Double) =
    get(name, Double::class.java, optional = true, nullable = false, convertable = true) { defaultValue } as Double

fun Raw.getAsDoubleOrFail(name: String) =
    get(name, Double::class.java, optional = false, nullable = false, convertable = true) as Double

fun Raw.getAsDoubleOrNull(name: String) =
    get(name, Double::class.java, optional = true, nullable = true, convertable = true) as Double?

private val doubleListType = typeOf<List<Double>>().javaType

@JvmOverloads
fun Raw.getDoubleList(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    doubleListType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as List<Double>

fun Raw.getDoubleListOrDefault(name: String, defaultValue: List<Double>) =
    get(name, doubleListType, optional = true, nullable = false) { defaultValue } as List<Double>

fun Raw.getDoubleListOrFail(name: String) =
    get(name, doubleListType, optional = false, nullable = false) as List<Double>

fun Raw.getDoubleListOrNull(name: String) = get(name, doubleListType, optional = true, nullable = true) as List<Double>?

private val doubleSetType = typeOf<Set<Double>>().javaType

@JvmOverloads
fun Raw.getDoubleSet(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    doubleSetType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Set<Double>

fun Raw.getDoubleSetOrDefault(name: String, defaultValue: Set<Double>) =
    get(name, doubleSetType, optional = true, nullable = false) { defaultValue } as Set<Double>

fun Raw.getDoubleSetOrFail(name: String) = get(name, doubleSetType, optional = false, nullable = false) as Set<Double>
fun Raw.getDoubleSetOrNull(name: String) = get(name, doubleSetType, optional = true, nullable = true) as Set<Double>?

private val doubleCollectionType = typeOf<Collection<Double>>().javaType

@JvmOverloads
fun Raw.getDoubleCollection(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    doubleCollectionType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Collection<Double>

fun Raw.getDoubleCollectionOrDefault(name: String, defaultValue: Collection<Double>) =
    get(name, doubleCollectionType, optional = true, nullable = false) { defaultValue } as Collection<Double>

fun Raw.getDoubleCollectionOrFail(name: String) =
    get(name, doubleCollectionType, optional = false, nullable = false) as Collection<Double>

fun Raw.getDoubleCollectionOrNull(name: String) =
    get(name, doubleCollectionType, optional = true, nullable = true) as Collection<Double>?

private val doubleIterableType = typeOf<Iterable<Double>>().javaType

@JvmOverloads
fun Raw.getDoubleIterable(name: String, defaultValueFactory: (() -> Any?)? = null) = get(
    name,
    doubleIterableType,
    optional = false,
    nullable = false,
    defaultValueFactory = defaultValueFactory
) as Iterable<Double>

fun Raw.getDoubleIterableOrDefault(name: String, defaultValue: Iterable<Double>) =
    get(name, doubleIterableType, optional = true, nullable = false) { defaultValue } as Iterable<Double>

fun Raw.getDoubleIterableOrFail(name: String) =
    get(name, doubleIterableType, optional = false, nullable = false) as Iterable<Double>

fun Raw.getDoubleIterableOrNull(name: String) =
    get(name, doubleIterableType, optional = true, nullable = true) as Iterable<Double>?
