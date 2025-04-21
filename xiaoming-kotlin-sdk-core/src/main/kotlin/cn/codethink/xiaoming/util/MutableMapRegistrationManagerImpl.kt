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

class MutableMapRegistrationManagerImpl<K, E>(
    data: MutableMap<K, MapRegistration<K, E>> = ConcurrentHashMap()
) : AbstractMutableMapRegistrationManager<K, E, MapRegistration<K, E>>(data) {
    private inner class MutableMapRegistrationImpl(
        override val key: K,
        override val value: E,
        override val operation: Operation
    ) : MutableMapRegistration<K, E> {
        override val isRemoved: Boolean get() = getRegistration(key) == this

        override fun remove() {
            check(tryRemove()) { "Failed to remove registration for key: $key" }
        }

        override fun tryRemove(): Boolean {
            return data.computeIfPresent(key) { _, v ->
                if (v == this) {
                    null
                } else {
                    v
                }
            } == null
        }

        override fun ensureRemoved() {
            tryRemove()
        }
    }

    fun register(key: K, value: E, operation: Operation): MutableMapRegistration<K, E> {
        val registration = MutableMapRegistrationImpl(
            key = key,
            value = value,
            operation = operation
        )

        put(key, registration)
        return registration
    }
}