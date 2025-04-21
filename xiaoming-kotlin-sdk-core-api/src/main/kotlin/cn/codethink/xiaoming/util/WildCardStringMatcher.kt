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

@file:JvmName("WildcardStringMatchers")
@file:OptIn(InternalApi::class, JavaFriendlyApi::class)

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.api.CoreApi

interface WildCardStringMatcher : StringMatcher {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        val MAJORITY_OPTIONAL = of(majority = true, optional = true)

        @JvmStatic
        @JavaFriendlyApi
        val MINORITY_OPTIONAL = of(majority = false, optional = true)

        @JvmStatic
        @JavaFriendlyApi
        val MAJORITY_REQUIRED = of(majority = true, optional = false)

        @JvmStatic
        @JavaFriendlyApi
        val MINORITY_REQUIRED = of(majority = false, optional = false)

        @JvmStatic
        fun of(majority: Boolean, optional: Boolean): WildCardStringMatcher {
            return CoreApi.getInstance().createWildcardStringMatcher(majority, optional)
        }
    }

    val majority: Boolean
    val optional: Boolean
}

val MajorityOptionalWildCardStringMatcher: WildCardStringMatcher
    get() = WildCardStringMatcher.MAJORITY_OPTIONAL

val MajorityRequiredWildCardStringMatcher: WildCardStringMatcher
    get() = WildCardStringMatcher.MAJORITY_REQUIRED

val MinorityOptionalWildCardStringMatcher: WildCardStringMatcher
    get() = WildCardStringMatcher.MINORITY_OPTIONAL

val MinorityRequiredWildCardStringMatcher: WildCardStringMatcher
    get() = WildCardStringMatcher.MINORITY_REQUIRED
