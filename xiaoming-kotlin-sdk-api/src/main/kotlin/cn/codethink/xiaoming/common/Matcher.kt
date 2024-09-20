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

/**
 * Serializable tools to match objects.
 *
 * @author Chuanwise
 */
interface Matcher<out T> {
    val type: String

    @get:JsonIgnore
    val targetType: Class<@UnsafeVariance T>

    @get:JsonIgnore
    val targetNullable: Boolean
        get() = false

    fun isMatched(target: @UnsafeVariance T): Boolean
}

fun <T> Matcher<T>.isMatchable(target: Any?): Boolean {
    return targetType.isInstance(target) || (target == null && targetNullable)
}

fun <T> Matcher<T>.isMatchedOrNull(target: Any?): Boolean? {
    return if (isMatchable(target)) {
        (this as Matcher<Any?>).isMatched(target)
    } else {
        null
    }
}

fun Any?.isMatchedOrEqualsTo(matcherOrValue: Any?): Boolean? {
    return if (matcherOrValue is Matcher<*>) {
        matcherOrValue.isMatchedOrNull(this)
    } else {
        this == matcherOrValue
    }
}

interface LiteralMatcher<T> : Matcher<T> {
    val value: T
    override fun isMatched(target: T): Boolean = value == target
}

@Suppress("UNCHECKED_CAST")
object AnyMatcher : Matcher<Any?> {
    override val type: String = MATCHER_TYPE_ANY
    override val targetType: Class<Any?>
        get() = Any::class.java as Class<Any?>

    override fun isMatched(target: Any?): Boolean = true
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any?> AnyMatcher(): Matcher<T> = AnyMatcher as Matcher<T>

data class RegexStringMatcher(
    val regex: Regex
) : Matcher<String> {
    override val type: String
        get() = STRING_MATCHER_TYPE_REGEX
    override val targetType: Class<String>
        get() = String::class.java

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
}

data class LiteralStringMatcher(
    override val value: String
) : LiteralMatcher<String> {
    override val type: String
        get() = STRING_MATCHER_TYPE_LITERAL
    override val targetType: Class<String>
        get() = String::class.java
}
