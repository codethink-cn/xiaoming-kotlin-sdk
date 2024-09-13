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

package cn.codethink.xiaoming.common

import java.util.concurrent.ConcurrentHashMap

/**
 * Container of a registrable object.
 *
 * @author Chuanwise
 * @see SimpleRegistration
 */
interface Registration<T> {
    val value: T
    val subject: Subject
}

/**
 * Simple implementation of [Registration].
 *
 * @author Chuanwise
 */
class SimpleRegistration<T>(
    override val value: T,
    override val subject: Subject
) : Registration<T>


/**
 * Registrations is a container of [Registration]s. It can register and unregister registrations.
 *
 * @author Chuanwise
 */
interface Registrations<T> {
    fun unregisterBySubject(subject: Subject): Boolean
}

/**
 * Registrations in this class are associated with a key.
 *
 * @author Chuanwise
 */
class MapRegistrations<K, T, R : Registration<T>> : Registrations<T> {
    val map: MutableMap<K, R> = ConcurrentHashMap()
    val keys: Set<K>
        get() = map.keys

    operator fun get(key: K): R? {
        return map[key]
    }

    fun register(key: K, registration: R): R? = map.put(key, registration)
    fun unregisterByKey(key: K): R? = map.remove(key)
    override fun unregisterBySubject(subject: Subject): Boolean = map.values.removeIf { it.subject == subject }
}