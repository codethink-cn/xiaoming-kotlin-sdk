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

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference


@InternalApi
class MutableDirectedAcyclicGraphImpl<N, E> : MutableDirectedAcyclicGraph<N, E> {
    private class NodeImpl<N, E>(override var value: N) : MutableDirectedAcyclicGraph.MutableNode<N, E> {
        override val outgoingEdges = mutableListOf<MutableDirectedAcyclicGraph.MutableEdge<N, E>>()
        override val incomingEdges = mutableListOf<MutableDirectedAcyclicGraph.MutableEdge<N, E>>()

        override val directPredecessors: List<DirectedAcyclicGraph.Node<N, E>> get() = incomingEdges.map { it.tail }
        override val directSuccessors: List<DirectedAcyclicGraph.Node<N, E>> get() = outgoingEdges.map { it.head }

        override val allPredecessors: List<DirectedAcyclicGraph.Node<N, E>> get() = transitiveClosure(directPredecessors) { it.directPredecessors } + directPredecessors
        override val allSuccessors: List<DirectedAcyclicGraph.Node<N, E>> get() = transitiveClosure(directSuccessors) { it.directSuccessors } + directSuccessors

        override fun toString(): String = "Node(value=$value, incoming=${directPredecessors.size}, outgoing=${directSuccessors.size})"
    }

    private class EdgeImpl<N, E>(
        override var value: E,
        override val tail: MutableDirectedAcyclicGraph.MutableNode<N, E>,
        override val head: MutableDirectedAcyclicGraph.MutableNode<N, E>
    ) : MutableDirectedAcyclicGraph.MutableEdge<N, E> {
        override fun toString(): String = "Edge(value=$value, tail=$tail, head=$head)"
    }

    override val nodes: MutableList<MutableDirectedAcyclicGraph.MutableNode<N, E>> = mutableListOf()
    override val edges: MutableList<MutableDirectedAcyclicGraph.MutableEdge<N, E>> = mutableListOf()

    private val roots: List<MutableDirectedAcyclicGraph.MutableNode<N, E>> get() = nodes.filter { it.directPredecessors.isEmpty() }

    override fun allocate(value: N): MutableDirectedAcyclicGraph.MutableNode<N, E> {
        return NodeImpl<N, E>(value).also {
            nodes.add(it)
        }
    }

    override fun link(
        tail: MutableDirectedAcyclicGraph.MutableNode<N, E>,
        head: MutableDirectedAcyclicGraph.MutableNode<N, E>,
        value: E
    ): MutableDirectedAcyclicGraph.MutableEdge<N, E> {
        tail as NodeImpl
        head as NodeImpl

        val allPredecessors = tail.allPredecessors
        require(!allPredecessors.contains(head)) {
            "Cannot link $tail to $head, because it is already a predecessor of $head. All predecessors: $allPredecessors"
        }

        val edge = EdgeImpl(value, tail, head)

        tail.outgoingEdges.add(edge)
        head.incomingEdges.add(edge)

        edges.add(edge)

        return edge
    }

    private class TraversingContext<N, E>(val node: NodeImpl<N, E>) {
        private enum class State {
            NOT_VISITED,
            VISITING,
            VISITED
        }

        private val atomicState = AtomicReference<State>(State.NOT_VISITED)

        fun isVisited(): Boolean {
            return atomicState.get() == State.VISITED
        }

        fun trySetVisiting(): Boolean {
            return atomicState.compareAndSet(State.NOT_VISITED, State.VISITING)
        }

        fun setVisited(): Boolean {
            return atomicState.compareAndSet(State.VISITING, State.VISITED)
        }
    }

    override suspend fun forEachConcurrently(block: suspend (DirectedAcyclicGraph.Node<N, E>) -> Unit) {
        val contexts = nodes.associateWith { TraversingContext(it as NodeImpl<N, E>) }

        coroutineScope {
            suspend fun traverse(context: TraversingContext<N, E>) {
                if (!context.trySetVisiting()) {
                    return
                }

                try {
                    block(context.node)
                } finally {
                    context.setVisited()

                    for (outgoingEdge in context.node.outgoingEdges) {
                        // 找到下一个节点。
                        val successorNode = outgoingEdge.head
                        val successorContext = contexts[successorNode] ?: continue
                        if (successorContext.isVisited()) {
                            continue
                        }

                        // 若该节点可以访问，则创建协程。
                        val directPredecessors = successorNode.directPredecessors
                        val directPredecessorContexts = directPredecessors.mapNotNull { contexts[it] }
                        if (!directPredecessorContexts.all { it.isVisited() }) {
                            continue
                        }

                        launch {
                            traverse(successorContext)
                        }
                    }
                }
            }

            for (root in roots) {
                val context = contexts[root] ?: continue
                launch {
                    traverse(context)
                }
            }
        }
    }

    override fun forEach(block: (DirectedAcyclicGraph.Node<N, E>) -> Unit) {
        val contexts = nodes.associateWith { TraversingContext(it as NodeImpl<N, E>) }

        val queue = ArrayDeque<TraversingContext<N, E>>()
        for (root in roots) {
            val context = contexts[root] ?: continue
            queue.add(context)
        }

        while (queue.isNotEmpty()) {
            val context = queue.removeFirst()
            if (!context.trySetVisiting()) {
                continue
            }

            block(context.node)

            context.setVisited()

            for (outgoingEdge in context.node.outgoingEdges) {
                // 找到下一个节点。
                val successorNode = outgoingEdge.head
                val successorContext = contexts[successorNode] ?: continue
                if (successorContext.isVisited()) {
                    continue
                }

                // 若该节点可以访问，则添加到队列。
                val directPredecessors = successorNode.directPredecessors
                val directPredecessorContexts = directPredecessors.mapNotNull { contexts[it] }
                if (!directPredecessorContexts.all { it.isVisited() }) {
                    continue
                }

                queue.add(successorContext)
            }
        }
    }
}

@InternalApi
fun <T> transitiveClosure(nodes: Iterable<T>, closure: (T) -> List<T>): List<T> {
    val result = mutableSetOf<T>()
    val queue = ArrayDeque<T>()
    queue.addAll(nodes)

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