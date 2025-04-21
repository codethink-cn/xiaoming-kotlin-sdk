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

import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractMutableListRegistrationManager<E, R : Registration<E>>(
    private val data: MutableList<R> = CopyOnWriteArrayList()
) : MutableListRegistrationManager<E, R> {
    override val elements: List<E> get() = toElementList()
    override val registrations: List<Registration<E>> get() = toMutableRegistrationList()

    override val size: Int get() = data.size
    override val isEmpty: Boolean get() = data.isEmpty()

    override fun toMutableRegistrationList(): MutableList<R> = data

    override fun toElementList(): List<E> = data.map { it.value }

    override fun toRegistrationList(): List<Registration<E>> = data.toList()

    override fun getElement(index: Int): E? = data.getOrNull(index)?.value

    override fun getRegistration(index: Int): Registration<E>? = data.getOrNull(index)

    override fun register(registration: R): Int {
        data.add(registration)
        return data.size - 1
    }

    override fun unregisterAll(operator: SubjectDescriptor): Boolean {
        return data.removeIf { it.operation.operator == operator }
    }
}