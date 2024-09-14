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

/**
 * Used to match a target.
 *
 * @author Chuanwise
 */
interface Matcher<T> {
    val type: String
    fun isMatched(target: T): Boolean
}

interface LiteralMatcher<T> : Matcher<T> {
    val value: T
    override fun isMatched(target: T): Boolean = value == target
}

/**
 * Match any amount of texts.
 *
 * Notice that:
 *
 * 1. It can not appear continuously.
 * 2. The next matcher can not be [GreedilyWildcardStringMatcher] or [WildcardStringMatcher].
 *
 * If [optional] is true, the matched texts can be empty. If [majority] is true,
 *
 * @author Chuanwise
 */
class GreedilyWildcardStringMatcher private constructor(
    val majority: Boolean,
    val optional: Boolean
) : Matcher<String>, DefaultStringListMatcherConstructingCallbackSupport,
    DefaultStringListMatcherMatchingCallbackSupport {
    companion object {
        @JvmStatic
        val MAJORITY_OPTIONAL = GreedilyWildcardStringMatcher(true, true)

        @JvmStatic
        val MINORITY_OPTIONAL = GreedilyWildcardStringMatcher(false, true)

        @JvmStatic
        val MAJORITY_REQUIRED = GreedilyWildcardStringMatcher(true, false)

        @JvmStatic
        val MINORITY_REQUIRED = GreedilyWildcardStringMatcher(false, false)

        @JvmStatic
        fun of(majority: Boolean, optional: Boolean): GreedilyWildcardStringMatcher = if (majority) {
            if (optional) MAJORITY_OPTIONAL else MAJORITY_REQUIRED
        } else {
            if (optional) MINORITY_OPTIONAL else MINORITY_REQUIRED
        }
    }

    override val type: String = TEXT_MATCHER_TYPE_GREEDILY_WILDCARD
    override fun isMatched(target: String): Boolean = true

    override fun onDefaultStringListMatcherConstructing(context: DefaultStringListMatcherConstructingContext) {
        if (context.matcherIndex > 0) {
            // If it is not the first one, check if previous one is `GreedilyWildcardStringMatcher`.
            val previous = context.matchers[context.matcherIndex - 1]
            if (previous is GreedilyWildcardStringMatcher) {
                throw IllegalArgumentException(
                    "OmittedSegmentMatcher next to GreedilyOmittedSegmentMatcher near index ${context.matcherIndex}!"
                )
            }
        }
        if (context.matcherIndex < context.matchers.size - 1) {
            // If it is not the last one, check if next one is `GreedilyWildcardStringMatcher` or `WildcardStringMatcher`.
            val next = context.matchers[context.matcherIndex + 1]
            if (next is GreedilyWildcardStringMatcher || next is WildcardStringMatcher) {
                throw IllegalArgumentException(
                    "GreedilyOmittedSegmentMatcher appear continuously near index ${context.matcherIndex}!"
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

        // The matcher after GreedilyOmittedSegmentMatcher must be functional.
        if (nextMatcher is GreedilyWildcardStringMatcher || nextMatcher is WildcardStringMatcher) {
            throw IllegalArgumentException(
                "GreedilyOmittedSegmentMatcher can not followed " +
                        "by another GreedilyWildcardTextMatcher or WildcardTextMatcher."
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

val MajorityOptionalGreedilyWildcardStringMatcher: GreedilyWildcardStringMatcher
    get() = GreedilyWildcardStringMatcher.MAJORITY_OPTIONAL

val MajorityRequiredGreedilyWildcardStringMatcher: GreedilyWildcardStringMatcher
    get() = GreedilyWildcardStringMatcher.MAJORITY_REQUIRED

val MinorityOptionalGreedilyWildcardStringMatcher: GreedilyWildcardStringMatcher
    get() = GreedilyWildcardStringMatcher.MINORITY_OPTIONAL

val MinorityRequiredGreedilyWildcardStringMatcher: GreedilyWildcardStringMatcher
    get() = GreedilyWildcardStringMatcher.MINORITY_REQUIRED

data object WildcardStringMatcher : Matcher<String> {
    override val type: String = TEXT_MATCHER_TYPE_WILDCARD
    override fun isMatched(target: String): Boolean = true
}

data class RegexStringMatcher(
    val regex: Regex
) : Matcher<String> {
    override val type: String = TEXT_MATCHER_TYPE_REGEX
    override fun isMatched(target: String): Boolean = regex.matches(target)
}

data class LiteralStringMatcher(
    override val value: String
) : LiteralMatcher<String> {
    override val type: String = TEXT_MATCHER_TYPE_LITERAL
}
