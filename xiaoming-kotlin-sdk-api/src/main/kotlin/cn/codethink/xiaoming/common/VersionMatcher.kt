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
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

const val VERSION_MATCHER_TYPE = "version"

/**
 * @author Chuanwise
 * @see toVersionMatcher
 */
@JsonSerialize(using = VersionMatcherSerializer::class)
@JsonDeserialize(using = VersionMatcherDeserializer::class)
sealed interface VersionMatcher : Matcher<Version> {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String) = string.toVersionMatcher()
    }

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

object VersionMatcherSerializer : StdSerializer<VersionMatcher>(VersionMatcher::class.java) {
    private fun readResolve(): Any = VersionMatcherSerializer
    override fun serialize(value: VersionMatcher, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(value.toString())
    }
}

object VersionMatcherDeserializer : StdDeserializer<VersionMatcher>(VersionMatcher::class.java) {
    private fun readResolve(): Any = VersionMatcherDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): VersionMatcher {
        return parser.valueAsString.toVersionMatcher()
    }
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
 * versionMatcher := singleVersionMatcher               // Match the single version matcher.
 *                 | "(" versionMatcher ")"             // Match the version matcher in parentheses.
 *                 | versionMatcher "&" versionMatcher  // Match the version matcher that is matched by both matchers.
 *                 | versionMatcher "|" versionMatcher  // Match the version matcher that is matched by either matcher.
 *                 ;
 * ```
 */
fun String.toVersionMatcher(): VersionMatcher {
    require(isNotEmpty()) {
        "Version matcher string must not be empty."
    }

    // 1. Tokenize.
    abstract class Token
    class OperatorToken(val operator: String) : Token()
    class VersionMatcherToken(val matcher: String) : Token()

    val tokens = mutableListOf<Token>()
    val current = StringBuilder()

    fun String.toToken(): Token = when (this) {
        "(", ")", "&", "|" -> OperatorToken(this)
        else -> VersionMatcherToken(this)
    }
    for (char in this) {
        if (char == ' ') {
            continue
        }

        when (char) {
            '(', ')', '&', '|' -> {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString().toToken())
                    current.clear()
                }

                tokens.add(char.toString().toToken())
            }

            else -> current.append(char)
        }
    }

    if (current.isNotEmpty()) {
        tokens.add(current.toString().toToken())
    }

    // 2. Parse.
    val stack = mutableListOf<VersionMatcher>()
    val operatorStack = mutableListOf<String>()

    // After tokenize, all possible tokens:
    // &, |, (, ) matcher.

    fun popUntilLeftParentheses() {
        while (operatorStack.isNotEmpty() && operatorStack.last() != "(") {
            val operator = operatorStack.removeLast()
            val right = stack.removeLast()
            val left = stack.removeLast()

            stack.add(
                when (operator) {
                    "&" -> AndVersionMatcher(left, right)
                    "|" -> OrVersionMatcher(left, right)
                    else -> throw IllegalArgumentException("Invalid operator: $operator")
                }
            )
        }

        if (operatorStack.isNotEmpty()) {
            operatorStack.removeLast()
        }
    }

    for (token in tokens) {
        when (token) {
            is VersionMatcherToken -> stack.add(token.matcher.toSingleStringMatcher())
            is OperatorToken -> when (token.operator) {
                "(" -> operatorStack.add(token.operator)
                ")" -> popUntilLeftParentheses()
                "&", "|" -> {
                    while (operatorStack.isNotEmpty() && operatorStack.last() != "(") {
                        val operator = operatorStack.removeLast()
                        val right = stack.removeLast()
                        val left = stack.removeLast()

                        stack.add(
                            when (operator) {
                                "&" -> AndVersionMatcher(left, right)
                                "|" -> OrVersionMatcher(left, right)
                                else -> throw IllegalArgumentException("Invalid operator: $operator")
                            }
                        )
                    }

                    operatorStack.add(token.operator)
                }

                else -> throw IllegalArgumentException("Invalid operator: ${token.operator}")
            }
        }
    }

    while (operatorStack.isNotEmpty()) {
        val operator = operatorStack.removeLast()
        val right = stack.removeLast()
        val left = stack.removeLast()

        stack.add(
            when (operator) {
                "&" -> AndVersionMatcher(left, right)
                "|" -> OrVersionMatcher(left, right)
                else -> throw IllegalArgumentException("Invalid operator: $operator")
            }
        )
    }

    require(stack.size == 1) {
        "Invalid version matcher: $this"
    }

    return stack.first()
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
 * BNF patterns:
 *
 * ```bnf
 * singleVersionMatcher := version                            // Match the exact version.
 *                       | not version                        // Match the version that is not equal to the version.
 *                       | versionRange                       // Match the version range.
 *                       | versionPrefix                      // Match the version prefix.
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
 * not := "!" | "~";
 *
 * greaterThan := ">";
 *
 * greaterThanOrEqual := ">=" | "=>" | "]";
 *
 * lessThan := "<";
 *
 * lessThanOrEqual := "<=" | "" | "]";
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
        startsWith('!') || startsWith('~') -> ExcludeVersionMatcher(substring(1).toVersion())

        // Greater than or equal.
        startsWith(">=") || startsWith("=>") -> GreaterThanOrEqualVersionMatcher(substring(2).toVersion())
        startsWith(']') -> GreaterThanOrEqualVersionMatcher(substring(1).toVersion())

        endsWith("<=") || endsWith("=<") -> GreaterThanOrEqualVersionMatcher(substring(0, length - 2).toVersion())
        endsWith('[') -> GreaterThanOrEqualVersionMatcher(substring(0, length - 1).toVersion())

        // Greater than.
        startsWith('>') -> GreaterThanVersionMatcher(substring(1).toVersion())
        endsWith('<') -> GreaterThanVersionMatcher(substring(0, length - 1).toVersion())

        // Less than or equal.
        startsWith("<=") || startsWith("=<") -> LessThanOrEqualVersionMatcher(substring(2).toVersion())
        startsWith('[') -> LessThanOrEqualVersionMatcher(substring(1).toVersion())

        endsWith(">=") || endsWith("=>") -> LessThanOrEqualVersionMatcher(substring(0, length - 2).toVersion())
        endsWith(']') -> LessThanOrEqualVersionMatcher(substring(0, length - 1).toVersion())

        // Less than.
        startsWith('<') -> LessThanVersionMatcher(substring(1).toVersion())
        endsWith('>') -> LessThanVersionMatcher(substring(0, length - 1).toVersion())

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

data object AnyVersionMatcher : VersionMatcher {
    override fun isMatched(target: Version): Boolean = true

    override fun toString(): String = ""
}

sealed interface SingleVersionMatcher : VersionMatcher

class AndVersionMatcher(
    val left: VersionMatcher,
    val right: VersionMatcher
) : VersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return left.isMatched(target) && right.isMatched(target)
    }

    override fun toString(): String = "($left & $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AndVersionMatcher

        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    private val hashCodeCache: Int = run {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        result
    }

    override fun hashCode(): Int = hashCodeCache
}

class OrVersionMatcher(
    val left: VersionMatcher,
    val right: VersionMatcher
) : VersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return left.isMatched(target) || right.isMatched(target)
    }

    override fun toString(): String = "($left | $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrVersionMatcher

        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    private val hashCodeCache: Int = run {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        result
    }

    override fun hashCode(): Int = hashCodeCache
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