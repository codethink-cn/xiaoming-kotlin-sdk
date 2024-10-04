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
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Container of a registrable object.
 *
 * @author Chuanwise
 * @see DefaultRegistration
 */
interface Registration<T> {
    val value: T
    val subject: SubjectDescriptor
}

/**
 * Default implementation of [Registration].
 *
 * @author Chuanwise
 */
class DefaultRegistration<T>(
    override val value: T,
    override val subject: SubjectDescriptor
) : Registration<T>


/**
 * Registrations is a container of [Registration]s. It can register and unregister registrations.
 *
 * @author Chuanwise
 */
interface Registrations {
    fun unregisterBySubject(subject: SubjectDescriptor): Boolean
}

/**
 * Registrations in this class are associated with a key.
 *
 * @author Chuanwise
 */
class MapRegistrations<K, T, R : Registration<T>> : Registrations {
    private val mutableMap: MutableMap<K, R> = ConcurrentHashMap()
    private val map: Map<K, R>
        get() = mutableMap.toMap()

    fun toMap(): Map<K, R> = map

    operator fun get(key: K): R? {
        return mutableMap[key]
    }

    fun register(key: K, registration: R) {
        mutableMap[key] = registration
    }
    fun unregisterByKey(key: K): R? = mutableMap.remove(key)
    override fun unregisterBySubject(subject: SubjectDescriptor): Boolean =
        mutableMap.values.removeIf { it.subject == subject }
}

inline fun <reified K, reified T> DefaultMapRegistrations() = MapRegistrations<K, T, DefaultRegistration<T>>()
inline fun <reified T> DefaultStringMapRegistrations() = DefaultMapRegistrations<String, T>()

/**
 * Registrations in this class are saved in a list.
 *
 * @author Chuanwise
 */
class ListRegistrations<T, R : Registration<T>> : Registrations {
    private val mutableList = CopyOnWriteArrayList<R>()
    private val list: List<R>
        get() = mutableList.toList()

    fun toList(): List<R> = list

    fun register(registration: R) {
        mutableList.add(registration)
    }

    fun unregisterByValue(value: T) = mutableList.removeIf { it.value == value }
    override fun unregisterBySubject(subject: SubjectDescriptor): Boolean =
        mutableList.removeIf { it.subject == subject }
}

inline fun <reified T> DefaultListRegistrations() = ListRegistrations<T, DefaultRegistration<T>>()
