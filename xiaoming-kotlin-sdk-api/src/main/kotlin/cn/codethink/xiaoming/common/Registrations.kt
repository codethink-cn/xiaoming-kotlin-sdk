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
class MapRegistrations<K, T, R : Registration<T>>(
    private val mutableMap: MutableMap<K, R> = ConcurrentHashMap()
) : Registrations, MutableMap<K, R> by mutableMap {
    fun toMap(): Map<K, R> = mutableMap.toMap()
    fun register(key: K, registration: R) {
        mutableMap[key] = registration
    }

    fun unregisterByKey(key: K): R? = mutableMap.remove(key)
    override fun unregisterBySubject(subject: SubjectDescriptor): Boolean =
        mutableMap.values.removeIf { it.subject == subject }
}

inline fun <reified K, reified T> DefaultMapRegistrations() = MapRegistrations<K, T, DefaultRegistration<T>>()
inline fun <reified T> DefaultStringMapRegistrations() = DefaultMapRegistrations<String, T>()
inline fun <reified T> DefaultIdMapRegistrations() = DefaultMapRegistrations<Id, T>()

/**
 * Registrations in this class are saved in a list.
 *
 * @author Chuanwise
 */
class ListRegistrations<T, R : Registration<T>>(
    private val mutableList: MutableList<R> = CopyOnWriteArrayList<R>()
) : Registrations, MutableList<R> by mutableList {
    fun toList(): List<R> = mutableList.toList()
    fun register(registration: R) {
        mutableList.add(registration)
    }

    fun unregisterByValue(value: T) = mutableList.removeIf { it.value == value }
    override fun unregisterBySubject(subject: SubjectDescriptor): Boolean =
        mutableList.removeIf { it.subject == subject }
}

inline fun <reified T> DefaultListRegistrations() = ListRegistrations<T, DefaultRegistration<T>>()
