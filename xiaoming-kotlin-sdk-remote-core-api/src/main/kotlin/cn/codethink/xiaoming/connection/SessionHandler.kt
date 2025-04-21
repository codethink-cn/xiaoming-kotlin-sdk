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

import cn.codethink.xiaoming.util.NotStableForInheritance
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 会话处理器。
 *
 * @author Chuanwise
 */
@NotStableForInheritance
interface SessionHandler {
    /**
     * 会话启动时调用该函数。
     *
     * @param context 会话启动上下文
     */
    @JvmBlockingBridge
    suspend fun onStart(context: SessionStartContext)

    /**
     * 会话请求时调用该函数。
     *
     * 该函数的实现应当调用一次 [SessionRequestContext] 中的请求结果函数以上报请求结果。
     * 否则请求将被视作失败。
     *
     * @param context 会话请求上下文
     * @see SessionRequestContext
     */
    @JvmBlockingBridge
    suspend fun onRequest(context: SessionRequestContext)

    /**
     * 会话被关闭前调用该函数。
     *
     * 哪怕抛出异常，会话也会被关闭。
     *
     * @param context 会话关闭上下文
     * @see SessionActiveStopContext
     * @see SessionPassiveStopContext
     */
    @JvmBlockingBridge
    suspend fun onStop(context: SessionStopContext)
}