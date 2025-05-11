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
 * 可编辑有向无环图
 *
 * @param N 节点元素类型
 * @param E 边元素类型
 * @author Chuanwise
 */
interface MutableDirectedAcyclicGraph<N, E> : DirectedAcyclicGraph<N, E> {
    /**
     * 可编辑有向无环图节点
     *
     * @param N 节点元素类型
     * @param E 边元素类型
     * @author Chuanwise
     */
    interface MutableNode<N, E> : DirectedAcyclicGraph.Node<N, E> {
        override var value: N
    }

    /**
     * 可编辑有向无环图边
     *
     * @param N 节点元素类型
     * @param E 边元素类型
     * @author Chuanwise
     */
    interface MutableEdge<N, E> : DirectedAcyclicGraph.Edge<N, E> {
        override var value: E
    }

    /**
     * 图上的所有节点。
     */
    override val nodes: List<MutableNode<N, E>>

    /**
     * 图上的所有边。
     */
    override val edges: List<MutableEdge<N, E>>

    /**
     * 分配一个节点
     *
     * @param value 节点值
     * @return 节点
     */
    fun allocate(value: N): MutableNode<N, E>

    /**
     * 链接两个节点
     *
     * @param tail 父节点
     * @param head 子节点
     * @param value 边值
     */
    fun link(tail: MutableNode<N, E>, head: MutableNode<N, E>, value: E): MutableEdge<N, E>
}