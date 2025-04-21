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

import java.util.function.Supplier

/**
 * 保存有类型数据的容器。
 *
 * @author Chuanwise
 * @see MutableStore
 * @see createMapStore
 * @see createEmptyStore
 */
@NotStableForInheritance
interface Store {
    /**
     * 获取原始数据的所有键。
     */
    val keys: Set<String>

    /**
     * 获取原始数据的所有值。
     */
    val size: Int

    /**
     * 获取原始数据的所有值。
     */
    val isEmpty: Boolean get() = size == 0

    /**
     * 通过给定的类型读取原始数据的字段。
     *
     * @param T 类型
     * @param key 键
     * @param defaultValueFactory 值不存在时，返回值工厂。`null` 表示不返回，抛出 [NoSuchElementException]。
     * @return 读取到的值
     * @throws NullPointerException 对应的值为 null 但 [key] 为不可空类型
     * @throws NoSuchElementException 不存在对应的值
     */
    fun <T> get(key: TypeKey<T>, defaultValueFactory: Supplier<T>?): T = get(key.name, key.meta, defaultValueFactory)

    /**
     * 通过给定的类型读取原始数据的字段。
     *
     * @param T 类型
     * @param name 键
     * @param meta 类型元数据
     * @return 读取到的值
     */
    fun <T> get(name: String, meta: TypeMeta<T>): T = get(name, meta, defaultValueFactory = null)

    /**
     * 通过给定的类型读取原始数据的字段。
     *
     * @param T 类型
     * @param name 键
     * @param meta 类型元数据
     * @param defaultValueFactory 值不存在时，返回值工厂。`null` 表示不返回，抛出 [NoSuchElementException]。
     * @return 读取到的值
     */
    fun <T> get(name: String, meta: TypeMeta<T>, defaultValueFactory: Supplier<T>?): T

    /**
     * 通过给定的类型读取原始数据的字段，如果错误则返回 null。
     *
     * @param T 类型
     * @param key 键
     * @return 读取到的值，或者 null
     */
    fun <T> getOrNull(key: TypeKey<T>): T? = getOrNull(key.name, key.meta)

    /**
     * 通过给定的类型读取原始数据的字段，如果错误则返回 null。
     *
     * @param T 类型
     * @param name 键
     * @param meta 类型元数据
     * @return 读取到的值，或者 null
     */
    fun <T> getOrNull(name: String, meta: TypeMeta<T>): T? = getOrNull(name, meta, defaultValueFactory = null)

    /**
     * 通过给定的类型读取原始数据的字段，如果错误则返回 null。
     *
     * @param T 类型
     * @param name 键
     * @param meta 类型元数据
     * @param defaultValueFactory 值不存在时创建的工厂
     * @return 读取到的值，或者 null
     */
    fun <T> getOrNull(name: String, meta: TypeMeta<T>, defaultValueFactory: Supplier<T>?): T?

    /**
     * 判断原始数据是否包含某个字段。
     *
     * @param key 字段名
     * @return 是否包含
     */
    operator fun contains(key: String): Boolean

    /**
     * 将存储内容通过 ", " 连接为字符串。
     *
     * @return 字符串
     */
    fun contentToString(): String
}