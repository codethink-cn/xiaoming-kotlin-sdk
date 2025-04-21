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

package cn.codethink.xiaoming.util

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeName

class WildCardStringMatcherImpl private constructor(
    override val majority: Boolean,
    override val optional: Boolean,
    private val toStringCache: String
) : WildCardStringMatcher {
    companion object {
        private val MAJORITY_OPTIONAL = WildCardStringMatcherImpl(majority = true, optional = true, toStringCache = "??")
        private val MINORITY_OPTIONAL = WildCardStringMatcherImpl(majority = false, optional = true, toStringCache = "?")
        private val MAJORITY_REQUIRED = WildCardStringMatcherImpl(majority = true, optional = false, toStringCache = "++")
        private val MINORITY_REQUIRED = WildCardStringMatcherImpl(majority = false, optional = false, toStringCache = "+")

        @JvmStatic
        @JsonCreator
        fun of(majority: Boolean, optional: Boolean): WildCardStringMatcher = when {
            majority && optional -> MAJORITY_OPTIONAL
            !majority && optional -> MINORITY_OPTIONAL
            majority && !optional -> MAJORITY_REQUIRED
            else -> MINORITY_REQUIRED
        }
    }

    override fun matches(string: String): Boolean = true

    override fun toString(): String = toStringCache
}