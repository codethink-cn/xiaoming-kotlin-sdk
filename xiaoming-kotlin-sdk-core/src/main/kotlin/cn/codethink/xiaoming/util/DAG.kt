/*
 * Copyright 2025 CodeThink Technologies and contributors.
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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@InternalApi
class DAG<T> {
    interface Node<T> {
        val value: T

        val parents: List<Node<T>>
        val sons: List<Node<T>>
    }

    private class NodeImpl<T>(override val value: T) : Node<T> {
        override val sons = mutableListOf<Node<T>>()
        override val parents = mutableListOf<Node<T>>()

        override fun toString(): String = "Node(value=$value)"
    }

    private val nodes = mutableListOf<Node<T>>()

    val roots: List<Node<T>> get() = nodes.filter { (it as NodeImpl).parents.isEmpty() }
    val leaves: List<Node<T>> get() = nodes.filter { (it as NodeImpl).sons.isEmpty() }

    fun allocate(value: T): Node<T> {
        return NodeImpl(value).also {
            nodes.add(it)
        }
    }

    fun link(parent: Node<T>, son: Node<T>) {
        parent as NodeImpl
        son as NodeImpl

        val allParents = transitiveClosure(parent as Node<T>) { it.parents }
        require(!allParents.contains(son)) {
            "Cannot link $parent to $son, because it is already a parent of $son"
        }

        parent.sons.add(son)
        son.parents.add(parent)
    }

    private class TraversingNode<T>(val node: NodeImpl<T>) {
        enum class State {
            NOT_VISITED,
            VISITING,
            VISITED
        }

        private val atomicState = AtomicReference<State>(State.NOT_VISITED)

        val isVisited: Boolean get() = atomicState.get() == State.VISITED

        fun trySetVisiting(): Boolean {
            return atomicState.compareAndSet(State.NOT_VISITED, State.VISITING)
        }

        fun setVisited() {
            atomicState.set(State.VISITED)
        }
    }

    suspend fun traverse(scope: CoroutineScope, parentJob: Job? = null, block: suspend (Node<T>) -> Unit) {
        val traversingNodes = nodes.associateWith { TraversingNode(it as NodeImpl<T>) }

        val job = SupervisorJob(parentJob)
        val deferred = CompletableDeferred<Unit>()
        val traversingJobs = CopyOnWriteArrayList<Job>()

        suspend fun tryDoTraverse(node: TraversingNode<T>) {
            if (node.trySetVisiting()) {
                try {
                    block(node.node)
                } finally {
                    node.setVisited()

                    for (son in node.node.sons) {
                        val traversingSon = traversingNodes[son] ?: continue
                        val traversingParents = traversingSon.node.parents.mapNotNull { traversingNodes[it] }
                        if (!traversingParents.all { it.isVisited }) {
                            continue
                        }

                        val traversingJob = scope.launch(job) {
                            tryDoTraverse(traversingSon)
                        }
                        traversingJobs.add(traversingJob)
                    }

                    if (traversingJobs.size == traversingNodes.size) {
                        deferred.complete(Unit)
                    }
                }
            }
        }

        val roots = traversingNodes.filter { it.key.parents.isEmpty() }
        for (root in roots) {
            val traversingJob = scope.launch(job) {
                tryDoTraverse(root.value)
            }
            traversingJobs.add(traversingJob)
        }

        deferred.await()

        for (traversingJob in traversingJobs) {
            traversingJob.join()
        }
    }

    fun traverse(block: (Node<T>) -> Unit) {
        val traversingNodes = nodes.associateWith { TraversingNode(it as NodeImpl<T>) }

        fun tryDoTraverse(node: TraversingNode<T>) {
            if (node.trySetVisiting()) {
                try {
                    block(node.node)
                } finally {
                    node.setVisited()

                    for (son in node.node.sons) {
                        val traversingSon = traversingNodes[son] ?: continue
                        val traversingParents = traversingSon.node.parents.mapNotNull { traversingNodes[it] }
                        if (!traversingParents.all { it.isVisited }) {
                            continue
                        }

                        tryDoTraverse(traversingSon)
                    }
                }
            }
        }

        val roots = traversingNodes.filter { it.key.parents.isEmpty() }
        for (root in roots) {
            tryDoTraverse(root.value)
        }
    }
}

@InternalApi
fun <T> transitiveClosure(node: T, closure: (T) -> List<T>): List<T> {
    val result = mutableSetOf(node)
    val queue = ArrayDeque<T>()
    queue.add(node)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        for (child in closure(current)) {
            if (result.add(child)) {
                queue.add(child)
            }
        }
    }

    return result.toList()
}