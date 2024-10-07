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

/**
 * Get all super classes and interfaces of the target class.
 *
 * @author Chuanwise
 */
private class AllSuperClassOrInterfacesView(
    val targetClass: Class<*>
) : Iterable<Class<*>> {
    private inner class It : Iterator<Class<*>> {
        private val visited = mutableSetOf<Class<*>>()
        private val queue = ArrayDeque<Class<*>>().apply {
            addLast(targetClass)
        }

        private fun removeVisitedClassInQueue() {
            if (queue.isEmpty()) {
                return
            }

            val current = queue.removeFirst()
            while (current in queue) {
                queue.removeFirst()
            }
            queue.addFirst(current)
        }

        override fun hasNext(): Boolean {
            removeVisitedClassInQueue()
            return queue.isNotEmpty()
        }

        override fun next(): Class<*> {
            if (!hasNext()) {
                throw NoSuchElementException()
            }

            val current = queue.removeFirst()
            visited.add(current)

            current.superclass?.takeIf { it !in visited }?.let { queue.addLast(it) }
            current.interfaces.filter { it !in visited }.forEach { queue.addLast(it) }
            return current
        }
    }

    override fun iterator(): Iterator<Class<*>> = It()
}

@InternalApi
val Class<*>.allAssignableClasses: Iterable<Class<*>>
    get() = AllSuperClassOrInterfacesView(this)