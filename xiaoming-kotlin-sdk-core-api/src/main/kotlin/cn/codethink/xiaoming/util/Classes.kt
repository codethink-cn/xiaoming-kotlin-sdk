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

@file:JvmName("Classes")

package cn.codethink.xiaoming.util

/**
 * 获取给定类型的所有父类及其实现的接口的工具。
 *
 * @author Chuanwise
 */
private class InheritedClassesWithDepth(
    val targetClass: Class<*>
) : Iterable<Pair<Int, Class<*>>> {
    private inner class IteratorImpl : Iterator<Pair<Int, Class<*>>> {
        private val visited = mutableSetOf<Class<*>>()
        private val queue = ArrayDeque<Pair<Int, Class<*>>>().apply {
            addLast(Pair(0, targetClass))
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

        override fun next(): Pair<Int, Class<*>> {
            if (!hasNext()) {
                throw NoSuchElementException()
            }

            val current = queue.removeFirst()
            visited.add(current.second)

            current.second.superclass?.takeIf { it !in visited }?.let { queue.addLast(Pair(current.first + 1, it)) }
            current.second.interfaces.filter { it !in visited }.forEach { queue.addLast(Pair(current.first + 1, it)) }
            return current
        }
    }

    override fun iterator(): Iterator<Pair<Int, Class<*>>> = IteratorImpl()
}

@InternalApi
val Class<*>.inheritedClassesWithDepth: Iterable<Pair<Int, Class<*>>>
    get() = InheritedClassesWithDepth(this)

private class MapIterator<T, U>(
    private val iterator: Iterator<T>,
    private val mapper: (T) -> U
) : Iterator<U> {
    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): U = mapper(iterator.next())
}

private class MapIterable<T, U>(
    private val iterable: Iterable<T>,
    private val mapper: (T) -> U
) : Iterable<U> {
    override fun iterator(): Iterator<U> = MapIterator(iterable.iterator(), mapper)
}

@InternalApi
val Class<*>.inheritedClasses: Iterable<Class<*>>
    get() = MapIterable(inheritedClassesWithDepth) { it.second }