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

@file:JvmName("VersionMatchers")

package cn.codethink.xiaoming.util

/**
 * @author Chuanwise
 * @see toVersionMatcher
 */
@NotStableForInheritance
interface VersionMatcher {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String) = string.toVersionMatcher()

        @JvmStatic
        @JavaFriendlyApi
        fun and(left: VersionMatcher, right: VersionMatcher) = AndVersionMatcher(left, right)

        @JvmStatic
        @JavaFriendlyApi
        fun or(left: VersionMatcher, right: VersionMatcher) = OrVersionMatcher(left, right)

        @JvmStatic
        @JavaFriendlyApi
        fun include(version: Version) = IncludeVersionMatcher(version)

        @JvmStatic
        @JavaFriendlyApi
        fun exclude(version: Version) = ExcludeVersionMatcher(version)

        @JvmStatic
        @JavaFriendlyApi
        fun greaterThan(version: Version) = GreaterThanVersionMatcher(version)

        @JvmStatic
        @JavaFriendlyApi
        fun greaterThanOrEqual(version: Version) = GreaterThanOrEqualVersionMatcher(version)

        @JvmStatic
        @JavaFriendlyApi
        fun lessThan(version: Version) = LessThanVersionMatcher(version)

        @JvmStatic
        @JavaFriendlyApi
        fun lessThanOrEqual(version: Version) = LessThanOrEqualVersionMatcher(version)

        @JvmStatic
        @JavaFriendlyApi
        fun major(major: Int) = MajorVersionPrefixMatcher(major)

        @JvmStatic
        @JavaFriendlyApi
        fun majorMinor(major: Int, minor: Int) = MajorMinorVersionPrefixMatcher(major, minor)
    }

    fun matches(version: Version): Boolean
}

fun String.toVersionMatcher(): VersionMatcher = VersionMatcher(this)

interface AndVersionMatcher : VersionMatcher {
    val left: VersionMatcher
    val right: VersionMatcher
}

interface OrVersionMatcher : VersionMatcher {
    val left: VersionMatcher
    val right: VersionMatcher
}

interface IncludeVersionMatcher : VersionMatcher {
    val value: Version
}

interface ExcludeVersionMatcher : VersionMatcher {
    val value: Version
}

interface RangeVersionMatcher : VersionMatcher {
    val version: Version
}

interface GreaterThanVersionMatcher : RangeVersionMatcher
interface GreaterThanOrEqualVersionMatcher : RangeVersionMatcher
interface LessThanVersionMatcher : RangeVersionMatcher
interface LessThanOrEqualVersionMatcher : RangeVersionMatcher

interface MajorVersionPrefixMatcher : VersionMatcher {
    val major: Int
}

interface MajorMinorVersionPrefixMatcher : VersionMatcher {
    val major: Int
    val minor: Int
}