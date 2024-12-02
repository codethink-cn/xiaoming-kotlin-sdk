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

import java.util.concurrent.ConcurrentHashMap

abstract class AbstractMutableMapRegistrationManager<K, E, R : Registration<E>>(
    private val data: MutableMap<K, R> = ConcurrentHashMap()
) : MutableMapRegistrationManager<K, E, R> {
    override val keys: Set<K> get() = data.keys

    override val elements: Collection<E> get() = data.values.map { it.value }
    override val registrations: Collection<R> get() = data.values

    override val size: Int get() = data.size
    override val isEmpty: Boolean get() = data.isEmpty()

    override fun toElementMap(): Map<K, E> = data.mapValues { it.value.value }

    override fun toRegistrationMap(): Map<K, R> = data

    override fun toMutableRegistrationMap(): MutableMap<K, R> = data

    override fun getElement(key: K): E? = data[key]?.value

    override fun getRegistration(key: K): R? = data[key]

    override fun unregisterAll(subject: SubjectDescriptor): Boolean {
        return data.values.removeIf { it.subject == subject }
    }

    override fun unregister(key: K): Boolean = data.remove(key) != null

    override fun register(key: K, registration: R): R? {
        return data.put(key, registration)
    }

    override fun clear(): Boolean = isEmpty.apply { clear() }
}