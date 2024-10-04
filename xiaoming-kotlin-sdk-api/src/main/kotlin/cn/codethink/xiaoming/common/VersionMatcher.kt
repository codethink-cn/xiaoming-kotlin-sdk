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

package cn.codethink.xiaoming.common

import com.fasterxml.jackson.annotation.JsonIgnore

const val VERSION_MATCHER_TYPE = "version"

/**
 * @author Chuanwise
 * @see toVersionMatcher
 */
sealed interface VersionMatcher : Matcher<Version> {
    @get:JsonIgnore
    override val type: String
        get() = VERSION_MATCHER_TYPE

    @get:JsonIgnore
    override val targetNullable: Boolean
        get() = false

    @get:JsonIgnore
    override val targetType: Class<Version>
        get() = Version::class.java
}

/**
 * Parse a string to a [VersionMatcher].
 *
 * Examples:
 *
 * ```
 * 1.0.0                    // Match the exact version.
 * !1.1.3, >=1.0.0, <2.0.0  // Match the version that is not equal to the version,
 *                          // greater than or equal to 1.0.0 and less than 2.0.0.
 * 1.0.+                    // Match the version prefix.
 * ```
 *
 * BNF patterns:
 *
 * ```bnf
 * versionMatcher := singleVersionMatcher | singleVersionMatcher "," versionMatcher;
 *
 * singleVersionMatcher := version        // Match the exact version.
 *                       | not version    // Match the version that is not equal to the version.
 *                       | versionRange   // Match the version range.
 *                       | versionPrefix  // Match the version prefix.
 *                       ;
 *
 * versionPrefix := digits:major "." digits:minor ".+"  // Match the version prefix.
 *                | digits:major ".+"                   // Match the major version.
 *                ;
 *
 * versionRange := simpleVersionRange;
 *
 * simpleVersionRange := greaterThan version         | version lessThan           // Greater than version.
 *                     | greaterThanOrEqual version  | version lessThanOrEqual    // Greater than or equal to version.
 *                     | lessThan version            | version greaterThan        // Less than version.
 *                     | lessThanOrEqual version     | version greaterThanOrEqual // Less than or equal to version.
 *                     ;
 *
 * not := "!" | "^";
 *
 * greaterThan := ">" | ")";
 *
 * greaterThanOrEqual := ">=" | "]";
 *
 * lessThan := "<" | ")";
 *
 * lessThanOrEqual := "<=" | "]";
 * ```
 */
fun String.toVersionMatcher(): VersionMatcher {
    require(isNotEmpty()) {
        "Version matcher string must not be empty."
    }

    val stringMatchers = split(",")
    check(stringMatchers.isNotEmpty()) {
        "Version matcher string must not be empty."
    }

    return if (stringMatchers.size == 1) {
        stringMatchers.single().toSingleStringMatcher()
    } else {
        CompositeVersionMatcher(stringMatchers.map { it.trim().toSingleStringMatcher() })
    }
}

private val MAJOR_PREFIX_REGEX = """(\d+)\.\+""".toRegex()
private val MAJOR_MINOR_PREFIX_REGEX = """(\d+)\.(\d+)\.\+""".toRegex()

/**
 * Parse a string to a [SingleVersionMatcher].
 *
 * Examples:
 *
 * ```
 * 1.0.0    // Match the exact version.
 * !1.0.0   // Match the version that is not equal to the version.
 * 1.0.+    // Match the version prefix.
 * 1.+      // Match the major version.
 * >1.0.0   // Greater than version.
 * >=1.0.0  // Greater than or equal to version.
 * <1.0.0   // Less than version.
 * <=1.0.0  // Less than or equal to version.
 * ```
 *
 * @see toVersionMatcher
 */
fun String.toSingleStringMatcher(): SingleVersionMatcher {
    require(isNotEmpty()) {
        "Version matcher string must not be empty."
    }

    return when {
        // Not equal.
        startsWith("!") || startsWith("^") -> ExcludeVersionMatcher(substring(1).toVersion())

        // Greater than or equal.
        startsWith(">=") -> GreaterThanOrEqualVersionMatcher(substring(2).toVersion())
        startsWith("]") -> GreaterThanOrEqualVersionMatcher(substring(1).toVersion())
        startsWith(">") || startsWith(")") -> GreaterThanVersionMatcher(substring(1).toVersion())

        // Less than or equal.
        startsWith("<=") -> LessThanOrEqualVersionMatcher(substring(2).toVersion())
        startsWith("[") -> LessThanOrEqualVersionMatcher(substring(1).toVersion())
        startsWith("<") || startsWith("(") -> LessThanVersionMatcher(substring(1).toVersion())

        // Prefix matcher.
        endsWith(".+") -> when {
            matches(MAJOR_MINOR_PREFIX_REGEX) -> {
                val (major, minor) = MAJOR_MINOR_PREFIX_REGEX.matchEntire(this)!!.destructured
                MajorMinorVersionPrefixMatcher(major.toInt(), minor.toInt())
            }

            matches(MAJOR_PREFIX_REGEX) -> {
                val (major) = MAJOR_PREFIX_REGEX.matchEntire(this)!!.destructured
                MajorVersionPrefixMatcher(major.toInt())
            }

            else -> throw IllegalArgumentException("Invalid version prefix: $this")
        }

        else -> IncludeVersionMatcher(toVersion())
    }
}

sealed interface SingleVersionMatcher : VersionMatcher

@JvmInline
value class CompositeVersionMatcher(
    private val matchers: List<VersionMatcher>
) : VersionMatcher {
    init {
        require(matchers.isNotEmpty()) {
            "Composite version matcher must have at least one matcher."
        }
    }

    override fun isMatched(target: Version): Boolean {
        return matchers.all { it.isMatched(target) }
    }

    override fun toString(): String = matchers.joinToString(", ")
}

@JvmInline
value class IncludeVersionMatcher(
    override val value: Version
) : LiteralMatcher<Version>, SingleVersionMatcher

@JvmInline
value class ExcludeVersionMatcher(
    val value: Version
) : SingleVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target != value
    }

    override fun toString(): String = "!$value"
}


sealed interface RangeVersionMatcher : SingleVersionMatcher

@JvmInline
value class GreaterThanVersionMatcher(
    val version: Version
) : VersionMatcher, RangeVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target > version
    }

    override fun toString(): String = ">$version"
}

@JvmInline
value class GreaterThanOrEqualVersionMatcher(
    val version: Version
) : VersionMatcher, RangeVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target >= version
    }

    override fun toString(): String = ">=$version"
}

@JvmInline
value class LessThanVersionMatcher(
    val version: Version
) : VersionMatcher, RangeVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target < version
    }

    override fun toString(): String = "<$version"
}

@JvmInline
value class LessThanOrEqualVersionMatcher(
    val version: Version
) : VersionMatcher, RangeVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target <= version
    }

    override fun toString(): String = "<=$version"
}

@JvmInline
value class MajorVersionPrefixMatcher(
    val major: Int
) : VersionMatcher, SingleVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target.major == major
    }

    override fun toString(): String = "$major.+"
}

class MajorMinorVersionPrefixMatcher(
    val major: Int,
    val minor: Int
) : VersionMatcher, SingleVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target.major == major && target.minor == minor
    }

    override fun toString(): String = "$major.$minor.+"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MajorMinorVersionPrefixMatcher

        if (major != other.major) return false
        if (minor != other.minor) return false

        return true
    }

    private val hashCodeCache = run {
        var result = major
        result = 31 * result + minor
        result
    }

    override fun hashCode(): Int = hashCodeCache
}