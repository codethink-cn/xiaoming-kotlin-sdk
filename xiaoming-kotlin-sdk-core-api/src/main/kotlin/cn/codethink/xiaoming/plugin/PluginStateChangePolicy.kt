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

import cn.codethink.xiaoming.util.JavaFriendlyApi

/**
 * 插件状态转换策略。
 *
 * @author Chuanwise
 */
interface PluginStateChangePolicy {
    companion object {
        @JvmStatic
        val STRICT = PluginStateChangePolicy(ignorePreviousError = false, ignoreCurrentError = false)

        @JvmStatic
        val IGNORE_PREVIOUS_ERROR = PluginStateChangePolicy(ignorePreviousError = true, ignoreCurrentError = false)

        @JvmStatic
        val IGNORE_CURRENT_ERROR = PluginStateChangePolicy(ignorePreviousError = false, ignoreCurrentError = true)

        @JvmStatic
        val IGNORE_ALL_ERROR = PluginStateChangePolicy(ignorePreviousError = true, ignoreCurrentError = true)

        @JvmStatic
        @JavaFriendlyApi
        fun of(ignorePreviousError: Boolean, ignoreCurrentError: Boolean): PluginStateChangePolicy =
            PluginStateChangePolicy(ignorePreviousError, ignoreCurrentError)
    }

    /**
     * 如果插件此时就处在错误状态，忽略此前的错误状态并重试。
     */
    val ignorePreviousError: Boolean

    /**
     * 本次操作若失败，是否忽略当前错误，认为操作成功。
     */
    val ignoreCurrentError: Boolean
}