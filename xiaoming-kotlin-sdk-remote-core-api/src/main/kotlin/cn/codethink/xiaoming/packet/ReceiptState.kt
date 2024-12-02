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

package cn.codethink.xiaoming.packet

import cn.codethink.xiaoming.util.InternalApi

/**
 * 回执状态。
 *
 * @author Chuanwise
 * @see ReceiptPacket.state
 */
enum class ReceiptState {
    /**
     * 接收方顺利完成请求动作。
     */
    SUCCEED,

    /**
     * 接收方尝试完成请求动作，但遇到了错误。
     */
    FAILED,

    /**
     * 只有 [RequestMode.FUTURE] 或 [RequestMode.ASYNC] 才有可能收到此回执，
     * 表示接收方收到请求动作，但是否已经开始执行取决于接收方。
     */
    RECEIVED,

    /**
     * 只有 [RequestMode.FUTURE] 才有可能收到此回执，
     * 表示接收方在执行请求动作时被中断。
     */
    INTERRUPTED,

    /**
     * 只有 [RequestMode.FUTURE] 才有可能收到此回执，
     * 接收方还未执行请求，因某些原因被取消。
     */
    CANCELLED;

    @InternalApi
    fun toLowerCaseString() = name.lowercase()

    @InternalApi
    fun fromLowerCaseString(string: String) = valueOf(string.uppercase())
}