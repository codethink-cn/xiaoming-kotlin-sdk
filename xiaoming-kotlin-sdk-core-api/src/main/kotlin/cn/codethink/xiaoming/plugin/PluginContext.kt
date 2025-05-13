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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.PlatformContext
import cn.codethink.xiaoming.event.EventContext
import cn.codethink.xiaoming.util.Operation
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

interface PluginContext : PlatformContext {
    val eventContext: EventContext<PluginEvent>
    val plugin: Plugin

    /**
     * 标记插件已产生无法恢复的严重错误，其状态混乱。
     *
     * @param operation 操作
     */
    @JvmBlockingBridge
    suspend fun crash(operation: Operation)

    /**
     * 标记插件已产生无法恢复的严重错误，其状态混乱。
     *
     * @param operation 操作
     */
    @JvmBlockingBridge
    suspend fun ensureCrashed(operation: Operation)
}