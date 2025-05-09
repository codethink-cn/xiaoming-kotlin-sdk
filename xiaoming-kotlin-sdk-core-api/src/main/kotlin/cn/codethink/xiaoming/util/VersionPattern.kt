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

@file:JvmName("VersionPatterns")

package cn.codethink.xiaoming.util

/**
 * @author Chuanwise
 * @see toVersionPattern
 */
@NotStableForInheritance
interface VersionPattern {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String) = string.toVersionPattern()

        @JvmStatic
        @JavaFriendlyApi
        fun and(left: VersionPattern, right: VersionPattern) = AndVersionPattern(left, right)

        @JvmStatic
        @JavaFriendlyApi
        fun or(left: VersionPattern, right: VersionPattern) = OrVersionPattern(left, right)

        @JvmStatic
        @JavaFriendlyApi
        fun include(version: Version) = IncludeVersionPattern(version)

        @JvmStatic
        @JavaFriendlyApi
        fun exclude(version: Version) = ExcludeVersionPattern(version)

        @JvmStatic
        @JavaFriendlyApi
        fun greaterThan(version: Version) = GreaterThanVersionPattern(version)

        @JvmStatic
        @JavaFriendlyApi
        fun greaterThanOrEqual(version: Version) = GreaterThanOrEqualVersionPattern(version)

        @JvmStatic
        @JavaFriendlyApi
        fun lessThan(version: Version) = LessThanVersionPattern(version)

        @JvmStatic
        @JavaFriendlyApi
        fun lessThanOrEqual(version: Version) = LessThanOrEqualVersionPattern(version)

        @JvmStatic
        @JavaFriendlyApi
        fun major(major: Int) = MajorVersionPrefixPattern(major)

        @JvmStatic
        @JavaFriendlyApi
        fun majorMinor(major: Int, minor: Int) = MajorMinorVersionPrefixPattern(major, minor)
    }

    fun matches(version: Version): Boolean
}

fun String.toVersionPattern(): VersionPattern = VersionPattern(this)

interface AndVersionPattern : VersionPattern {
    val left: VersionPattern
    val right: VersionPattern
}

interface OrVersionPattern : VersionPattern {
    val left: VersionPattern
    val right: VersionPattern
}

interface IncludeVersionPattern : VersionPattern {
    val value: Version
}

interface ExcludeVersionPattern : VersionPattern {
    val value: Version
}

interface RangeVersionPattern : VersionPattern {
    val version: Version
}

interface GreaterThanVersionPattern : RangeVersionPattern

interface GreaterThanOrEqualVersionPattern : RangeVersionPattern

interface LessThanVersionPattern : RangeVersionPattern

interface LessThanOrEqualVersionPattern : RangeVersionPattern

interface MajorVersionPrefixPattern : VersionPattern {
    val major: Int
}

interface MajorMinorVersionPrefixPattern : VersionPattern {
    val major: Int
    val minor: Int
}