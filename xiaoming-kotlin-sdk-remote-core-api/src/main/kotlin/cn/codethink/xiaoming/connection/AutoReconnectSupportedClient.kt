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

package cn.codethink.xiaoming.connection

/**
 * 支持重连的客户端。
 *
 * @author Chuanwise
 */
interface AutoReconnectSupportedClient<T> : Client<T> {
    val isPaused: Boolean

    /**
     * 启动自动连接任务。
     *
     * @throws IllegalStateException 此前已经启动过
     */
    fun start()

    /**
     * 启动自动连接任务。
     *
     * 若此前已经启动过，不会执行任何操作。
     */
    fun ensureStarted()

    /**
     * 暂停自动连接任务。
     *
     * @throws IllegalStateException 尚未被启动
     */
    fun pause()

    /**
     * 暂停自动连接任务。
     *
     * 若尚未被启动，不会执行任何动作。
     */
    fun ensurePaused()

    /**
     * 恢复自动连接任务。
     *
     * @throws IllegalStateException 尚未被暂停
     */
    fun resume()

    /**
     * 恢复自动连接任务。
     *
     * 若尚未被暂停，不会执行任何动作。
     */
    fun ensureResumed()
}