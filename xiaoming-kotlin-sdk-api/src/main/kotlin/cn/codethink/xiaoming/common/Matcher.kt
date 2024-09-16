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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Serializable tools to match objects.
 *
 * @author Chuanwise
 */
interface Matcher<T> {
    val type: String

    @get:JsonIgnore
    val targetType: Class<T>

    @get:JsonIgnore
    val targetNullable: Boolean
        get() = false

    fun isMatched(target: T): Boolean
}

fun <T> Matcher<T>.isMatchable(target: Any?): Boolean {
    return targetType.isInstance(target) || (target == null && targetNullable)
}

@Suppress("UNCHECKED_CAST")
fun <T> Matcher<T>.isMatchedOrNull(target: Any?): Boolean? {
    return if (isMatchable(target)) {
        (this as Matcher<Any?>).isMatched(target)
    } else {
        null
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

/**
 * Match any amount of texts.
 *
 * Notice that:
 *
 * 1. It can not appear continuously.
 * 2. The next matcher can not be [WildcardStringMatcher] or [AnyMatcher].
 *
 * If [optional] is true, the matched texts can be empty. If [majority] is true,
 *
 * @author Chuanwise
 */
class WildcardStringMatcher private constructor(
    val majority: Boolean,
    val optional: Boolean
) : Matcher<String>, DefaultStringListMatcherConstructingCallbackSupport,
    DefaultStringListMatcherMatchingCallbackSupport {
    companion object {
        @JvmStatic
        val MAJORITY_OPTIONAL = WildcardStringMatcher(majority = true, optional = true)

        @JvmStatic
        val MINORITY_OPTIONAL = WildcardStringMatcher(majority = false, optional = true)

        @JvmStatic
        val MAJORITY_REQUIRED = WildcardStringMatcher(majority = true, optional = false)

        @JvmStatic
        val MINORITY_REQUIRED = WildcardStringMatcher(majority = false, optional = false)

        @JvmStatic
        @JsonCreator
        fun of(majority: Boolean, optional: Boolean): WildcardStringMatcher = if (majority) {
            if (optional) MAJORITY_OPTIONAL else MAJORITY_REQUIRED
        } else {
            if (optional) MINORITY_OPTIONAL else MINORITY_REQUIRED
        }
    }

    override val type: String
        get() = STRING_MATCHER_TYPE_WILDCARD
    override fun isMatched(target: String): Boolean = true

    override val targetType: Class<String>
        get() = String::class.java

    override fun onDefaultStringListMatcherConstructing(context: DefaultStringListMatcherConstructingContext) {
        if (context.matcherIndex > 0) {
            // If it is not the first one, check if previous one is `WildcardStringMatcher`.
            val previous = context.matchers[context.matcherIndex - 1]
            if (previous == AnyMatcher<String>()) {
                throw IllegalArgumentException(
                    "AnyMatcher<String> next to WildcardStringMatcher near index ${context.matcherIndex}!"
                )
            }
        }
        if (context.matcherIndex < context.matchers.size - 1) {
            // If it is not the last one, check if next one is `WildcardStringMatcher` or `AnyMatcher<String>`.
            val next = context.matchers[context.matcherIndex + 1]
            if (next is WildcardStringMatcher || next !== AnyMatcher<String>()) {
                throw IllegalArgumentException(
                    "WildcardStringMatcher appear continuously near index ${context.matcherIndex}!"
                )
            }
        }
    }

    override fun onDefaultStringListMatcherMatching(context: DefaultStringListMatchingContext): Boolean {
        // If current matcher is the last one, matched.
        if (context.matcherIndex == context.matchers.size - 1) {
            context.result = true
            return true
        }

        context.matcherIndex++
        val nextMatcher = context.matchers[context.matcherIndex]

        // The matcher after WildcardStringMatcher must be functional.
        if (nextMatcher is WildcardStringMatcher || nextMatcher == AnyMatcher<String>()) {
            throw IllegalArgumentException(
                "WildcardStringMatcher can not followed " +
                        "by another WildcardStringMatcher or AnyMatcher<String>()."
            )
        }

        // Or get the next matcher and find matched segment.
        var nextSegmentId: Int
        if (majority) {
            nextSegmentId = context.target.size - 1
            while (nextSegmentId >= context.targetIndex) {
                val nextSegment = context.target[nextSegmentId]
                if (nextMatcher.isMatched(nextSegment)) {
                    break
                }
                nextSegmentId--
            }
            if ((nextSegmentId < context.targetIndex && optional) ||
                (nextSegmentId <= context.targetIndex && !optional)
            ) {
                context.result = false
                return false
            }
        } else {
            nextSegmentId = context.targetIndex
            while (nextSegmentId < context.target.size) {
                val nextSegment = context.target[nextSegmentId]
                if (nextMatcher.isMatched(nextSegment)) {
                    break
                }
                nextSegmentId++
            }
            if (nextSegmentId == context.targetIndex && !optional) {
                context.result = false
                return false
            }
        }

        context.matcherIndex++
        context.targetIndex = nextSegmentId + 1
        return true
    }
}

val MajorityOptionalWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MAJORITY_OPTIONAL

val MajorityRequiredWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MAJORITY_REQUIRED

val MinorityOptionalWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MINORITY_OPTIONAL

val MinorityRequiredWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MINORITY_REQUIRED

data class RegexStringMatcher(
    val regex: Regex
) : Matcher<String> {
    override val type: String = STRING_MATCHER_TYPE_REGEX
    override val targetType: Class<String>
        get() = String::class.java

    override fun isMatched(target: String): Boolean = regex.matches(target)
}

data class LiteralStringMatcher(
    override val value: String
) : LiteralMatcher<String> {
    override val type: String = STRING_MATCHER_TYPE_LITERAL
    override val targetType: Class<String>
        get() = String::class.java
}
