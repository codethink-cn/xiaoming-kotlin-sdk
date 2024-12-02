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

const val STRING_MATCHER_TYPE_WILDCARD = "string.wildcard"

@JsonTypeName(STRING_MATCHER_TYPE_WILDCARD)
class WildcardStringMatcherImpl private constructor(
    override val majority: Boolean,
    override val optional: Boolean,
    override val count: Int? = null
) : WildcardStringMatcher, DefaultStringListMatcherConstructingCallbackSupport,
    ListSegmentIdMatcherMatchingCallbackSupport {
    companion object {
        private val MAJORITY_OPTIONAL = WildcardStringMatcherImpl(majority = true, optional = true)

        private val MINORITY_OPTIONAL = WildcardStringMatcherImpl(majority = false, optional = true)

        private val MAJORITY_REQUIRED = WildcardStringMatcherImpl(majority = true, optional = false)

        private val MINORITY_REQUIRED = WildcardStringMatcherImpl(majority = false, optional = false)

        private val MINORITY_OPTIONAL_ONCE = WildcardStringMatcherImpl(majority = false, optional = true, count = 1)

        private val MINORITY_REQUIRED_ONCE = WildcardStringMatcherImpl(majority = false, optional = false, count = 1)

        @JvmStatic
        @JsonCreator
        fun of(majority: Boolean, optional: Boolean, count: Int? = null): WildcardStringMatcher = when (count) {
            null -> when (majority) {
                true -> if (optional) MAJORITY_OPTIONAL else MAJORITY_REQUIRED
                false -> if (optional) MINORITY_OPTIONAL else MINORITY_REQUIRED
            }

            1 -> when (majority) {
                true -> throw IllegalArgumentException("Majority can not be once.")
                false -> if (optional) MINORITY_OPTIONAL_ONCE else MINORITY_REQUIRED_ONCE
            }

            else -> WildcardStringMatcherImpl(majority, optional, count)
        }
    }

    init {
        count?.let {
            if (it <= 0 || majority) {
                throw IllegalArgumentException("If count provided, it should be positive and minority.")
            }
        }
    }

    val type: String = STRING_MATCHER_TYPE_WILDCARD

    override fun isMatched(target: String): Boolean = true

    override fun onListSegmentIdMatcherConstructing(context: ListSegmentIdMatcherConstructingContext) {
        if (context.matcherIndex > 0) {
            // If it is not the first one, check if previous one is `WildcardStringMatcher`.
            val previous = context.matchers[context.matcherIndex - 1]
            if (previous == AnyStringMatcher) {
                throw IllegalArgumentException(
                    "AnyMatcher<String> next to WildcardStringMatcher near index ${context.matcherIndex}!"
                )
            }
        }
        if (context.matcherIndex < context.matchers.size - 1) {
            // If it is not the last one, check if next one is `WildcardStringMatcher` or any optional matcher.
            val next = context.matchers[context.matcherIndex + 1]
            if (next is WildcardStringMatcher || next !== AnyStringMatcher) {
                throw IllegalArgumentException(
                    "WildcardStringMatcher appear continuously near index ${context.matcherIndex}!"
                )
            }
        }
    }

    override fun onListSegmentIdMatcherMatching(context: ListSegmentIdMatchingContext): Boolean {
        // If current matcher is the last one, and the element is the last one, matched.
        if (context.matcherIndex == context.matchers.size - 1) {
            if (context.targetIndex == context.target.size - 1) {
                context.result = true
                return true
            } else {
                context.result = count == null
                context.targetIndex++
                return count == null
            }
        }

        context.matcherIndex++
        val nextMatcher = context.matchers[context.matcherIndex]

        // The matcher after WildcardStringMatcher must be functional.
        if (nextMatcher is WildcardStringMatcher ||
            nextMatcher == AnyStringMatcher
        ) {

            throw IllegalArgumentException(
                "WildcardStringMatcher can not followed " +
                        "by another WildcardStringMatcher or AnyStringMatcher."
            )
        }

        // Or get the next matcher and find matched segment.
        var nextTargetIndex: Int
        if (majority) {
            // Majority.
            nextTargetIndex = context.target.size - 1
            while (nextTargetIndex >= context.targetIndex) {
                val nextSegment = context.target[nextTargetIndex]
                if (nextMatcher.isMatched(nextSegment)) {
                    break
                }
                nextTargetIndex--
            }
            if ((nextTargetIndex < context.targetIndex && optional) ||
                (nextTargetIndex <= context.targetIndex && !optional)
            ) {
                context.result = false
                return false
            }
            context.targetIndex = nextTargetIndex + 1
            context.matcherIndex++
        } else {
            // Minority.
            nextTargetIndex = context.targetIndex
            var count = count ?: 0
            while (nextTargetIndex < context.target.size) {
                val nextSegment = context.target[nextTargetIndex]
                if (nextMatcher.isMatched(nextSegment)) {
                    count--
                    if (count < 0) {
                        break
                    }
                }
                nextTargetIndex++
            }
            if (nextTargetIndex == context.targetIndex && !optional) {
                context.result = false
                return false
            }
            context.targetIndex = nextTargetIndex
        }

        return true
    }

    override fun onListSegmentIdMatcherMatchingRemaining(context: ListSegmentIdMatchingContext): Boolean {
        return optional
    }

    override fun onListSegmentIdMatcherMatchingEmpty(context: ListSegmentIdMatchingContext): Boolean {
        return optional
    }

    private val toStringCache: String by lazy {
        "WildcardStringMatcher(" +
                "majority=$majority," +
                "optional=$optional," +
                "count=$count)"
    }

    override fun toString(): String = toStringCache

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WildcardStringMatcher) return false

        if (majority != other.majority) return false
        if (optional != other.optional) return false
        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        var result = majority.hashCode()
        result = 31 * result + optional.hashCode()
        result = 31 * result + (count ?: 0)
        return result
    }
}