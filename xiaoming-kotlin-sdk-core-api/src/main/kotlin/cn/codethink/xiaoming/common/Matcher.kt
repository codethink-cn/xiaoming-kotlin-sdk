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

import com.fasterxml.jackson.annotation.JsonTypeName

/**
 * Serializable tools to match objects.
 *
 * @author Chuanwise
 */
interface Matcher<out T> {
    val type: String

    fun isMatched(target: @UnsafeVariance T): Boolean
}

interface LiteralMatcher<T> : Matcher<T> {
    val value: T
    override fun isMatched(target: T): Boolean = value == target
}

const val MATCHER_TYPE_ANY = "any"

@JsonTypeName(MATCHER_TYPE_ANY)
object AnyMatcher : Matcher<Any?> {
    override val type: String = MATCHER_TYPE_ANY

    override fun isMatched(target: Any?): Boolean = true
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any?> AnyMatcher(): Matcher<T> = AnyMatcher as Matcher<T>

const val MATCHER_TYPE_NONE = "none"

@JsonTypeName(MATCHER_TYPE_NONE)
object NoneMatcher : Matcher<Any?> {
    override val type: String = MATCHER_TYPE_NONE

    override fun isMatched(target: Any?): Boolean = false
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any?> NoneMatcher(): Matcher<T> = NoneMatcher as Matcher<T>

@JsonTypeName(STRING_MATCHER_TYPE_REGEX)
class RegexStringMatcher(
    val regex: Regex
) : Matcher<String> {
    override val type: String = STRING_MATCHER_TYPE_REGEX

    override fun isMatched(target: String): Boolean = regex.matches(target)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegexStringMatcher) return false

        if (regex.pattern != other.regex.pattern) return false

        return true
    }

    override fun hashCode(): Int {
        return regex.pattern.hashCode()
    }

    override fun toString(): String {
        return "RegexStringMatcher(regex=$regex)"
    }
}

@JsonTypeName(STRING_MATCHER_TYPE_LITERAL)
class LiteralStringMatcher(
    override val value: String
) : LiteralMatcher<String> {
    override val type: String = STRING_MATCHER_TYPE_LITERAL

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LiteralStringMatcher

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "LiteralStringMatcher(value='$value')"
    }
}
