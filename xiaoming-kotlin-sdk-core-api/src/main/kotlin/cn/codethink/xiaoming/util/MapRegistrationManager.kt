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

/**
 * 通过 [Map] 组织的注册管理器。
 *
 * @param K 键类型
 * @param E 元素类型
 * @author Chuanwise
 */
interface MapRegistrationManager<K, E> : RegistrationManager<E> {
    val keys: Set<K>
    override val registrations: Collection<MapRegistration<K, E>>

    fun toElementMap(): Map<K, E>
    fun toRegistrationMap(): Map<K, MapRegistration<K, E>>

    fun getElement(key: K): E?
    fun getRegistration(key: K): MapRegistration<K, E>?

    operator fun get(key: K): MapRegistration<K, E>? = getRegistration(key)
    operator fun contains(key: K): Boolean = getRegistration(key) != null
}
