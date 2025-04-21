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

import com.fasterxml.jackson.annotation.JsonCreator

class PluginStateChangePolicyImpl private constructor(
    override val ignorePreviousError: Boolean,
    override val ignoreCurrentError: Boolean
) : PluginStateChangePolicy {
    companion object {
        val STRICT = PluginStateChangePolicyImpl(
            ignorePreviousError = false,
            ignoreCurrentError = false
        )

        val IGNORE_PREVIOUS_ERROR = PluginStateChangePolicyImpl(
            ignorePreviousError = true,
            ignoreCurrentError = false
        )

        val IGNORE_CURRENT_ERROR = PluginStateChangePolicyImpl(
            ignorePreviousError = false,
            ignoreCurrentError = true
        )

        val IGNORE_ALL_ERROR = PluginStateChangePolicyImpl(
            ignorePreviousError = true,
            ignoreCurrentError = true
        )

        @JvmStatic
        @JsonCreator
        fun of(
            ignorePreviousError: Boolean = false, ignoreCurrentError: Boolean = false
        ): PluginStateChangePolicy {
            return PluginStateChangePolicyImpl(
                ignorePreviousError = ignorePreviousError,
                ignoreCurrentError = ignoreCurrentError
            )
        }
    }
}