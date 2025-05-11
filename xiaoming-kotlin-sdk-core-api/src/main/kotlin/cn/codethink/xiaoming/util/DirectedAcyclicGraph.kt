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

/**
 * 有向无环图
 *
 * @param N 节点元素类型
 * @param E 边元素类型
 * @author Chuanwise
 */
interface DirectedAcyclicGraph<N, E> {
    /**
     * 有向无环图节点
     *
     * @param N 节点元素类型
     * @param E 边元素类型
     * @author Chuanwise
     */
    interface Node<N, E> {
        val value: N

        val incomingEdges: List<Edge<N, E>>
        val outgoingEdges: List<Edge<N, E>>

        val directPredecessors: List<Node<N, E>>
        val directSuccessors: List<Node<N, E>>

        val allPredecessors: List<Node<N, E>>
        val allSuccessors: List<Node<N, E>>
    }

    /**
     * 有向无环图边
     *
     * @param N 节点元素类型
     * @param E 边元素类型
     * @author Chuanwise
     */
    interface Edge<N, E> {
        val value: E
        val tail: Node<N, E>
        val head: Node<N, E>
    }

    /**
     * 图上的所有节点。
     */
    val nodes: List<Node<N, E>>

    /**
     * 图上的所有边。
     */
    val edges: List<Edge<N, E>>

    /**
     * 按照拓扑排序顺序遍历图上的所有节点。
     *
     * @param block 遍历时的回调函数
     */
    fun forEach(block: (Node<N, E>) -> Unit)

    /**
     * 并发遍历图上的所有节点。
     *
     * @param block 遍历时的回调函数
     */
    suspend fun forEachConcurrently(block: suspend (Node<N, E>) -> Unit)
}