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

import cn.codethink.xiaoming.util.Action
import cn.codethink.xiaoming.util.AutoClosableSubject
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.NotStableForInheritance
import cn.codethink.xiaoming.util.Subject
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.nowTime
import kotlinx.coroutines.CoroutineScope
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 一个数据包会话，可以用于发送和接收请求。
 *
 * @author Chuanwise
 */
@NotStableForInheritance
interface Session : AutoClosableSubject, CoroutineScope {
    val isStopped: Boolean
    val isStopping: Boolean

    /**
     * 通过会话发送一次请求。
     *
     * @param P 请求参数类型
     * @param R 请求结果类型
     * @param action 请求动作
     * @param mode 请求模式
     * @param argument 请求参数
     * @param timeout 请求超时时间
     * @param cause 请求原因
     * @param subject 请求主体
     * @param time 请求时间
     * @return 请求结果
     * @throws InterruptedException 等待请求结果时中断
     */
    @JvmBlockingBridge
    @Throws(InterruptedException::class)
    suspend fun <P, R> request(
        action: Action<P, R>,
        argument: P?,
        timeout: Long,
        cause: Cause,
        subject: Subject,
        time: Time = nowTime,
    ): RequestResult<R>
}
