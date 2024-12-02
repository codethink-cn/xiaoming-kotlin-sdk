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
 * 请求模式。
 *
 * @author Chuanwise
 */
enum class RequestMode {
    /**
     * 接收方在完成请求，或出现错误时发送一次回执数据包。
     */
    SYNC,

    /**
     * 接收方在收到请求时立刻发送一次回执数据包，但既不保证动作被执行，也不保证执行结果可以知道。
     * 对于一些特殊的请求，也许其执行结果永远无法知悉。
     */
    ASYNC,

    /**
     * 接收方在收到请求时立刻发送一次回执数据包。在执行结束后再发送一次回执数据包。
     * 途中可能收到 [ReceiptState.INTERRUPTED] 的回执数据包。
     */
    FUTURE;

    @InternalApi
    fun toLowerCaseString() = name.lowercase()

    companion object {
        @InternalApi
        fun fromLowerCaseString(string: String) = valueOf(string.uppercase())
    }
}