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

import com.fasterxml.jackson.annotation.JsonTypeName

class ListSegmentIdMatchingContext(
    val target: SegmentId,
    val matchers: List<StringMatcher>,
    var matcherIndex: Int = 0,
    var targetIndex: Int = 0,
    var result: Boolean? = null
)

/**
 * If matcher implements this interface, it will be called when matching. Or [Matcher.isMatched] instead.
 *
 * Notice that implementations MUST maintain the index
 * [ListSegmentIdMatchingContext.matcherIndex] and [ListSegmentIdMatchingContext.targetIndex].
 *
 * @author Chuanwise
 */
interface ListSegmentIdMatcherMatchingCallbackSupport : Matcher<String> {
    fun onListSegmentIdMatcherMatching(context: ListSegmentIdMatchingContext): Boolean
    fun onListSegmentIdMatcherMatchingRemaining(context: ListSegmentIdMatchingContext): Boolean = false
    fun onListSegmentIdMatcherMatchingEmpty(context: ListSegmentIdMatchingContext): Boolean = false
}

class ListSegmentIdMatcherConstructingContext(
    val matchers: List<StringMatcher>,
    var matcherIndex: Int
)

/**
 * If matcher implements this interface, it will be called when constructing to check matchers.
 *
 * Notice that implementation DON'T HAVE TO maintain the index
 * [ListSegmentIdMatcherConstructingContext.matcherIndex]. If it's not changed, it will increase
 * automatically.
 *
 * @author Chuanwise
 */
interface DefaultStringListMatcherConstructingCallbackSupport : Matcher<String> {
    fun onListSegmentIdMatcherConstructing(context: ListSegmentIdMatcherConstructingContext)
}

const val SEGMENT_ID_MATCHER_TYPE_LIST = "segment_id.list"

@JsonTypeName(SEGMENT_ID_MATCHER_TYPE_LIST)
class ListSegmentIdMatcherImpl(
    private val matchers: List<StringMatcher>
) : SegmentIdMatcher {
    private val type: String = SEGMENT_ID_MATCHER_TYPE_LIST

    init {
        if (matchers.isEmpty()) {
            throw IllegalArgumentException("Segment matchers should not be empty.")
        }

        val context = ListSegmentIdMatcherConstructingContext(matchers, 0)
        while (context.matcherIndex < matchers.size) {
            if (matchers is DefaultStringListMatcherConstructingCallbackSupport) {
                // Text matcher can change the index. If not change, the index will increase automatically.
                val index = context.matcherIndex
                matchers.onListSegmentIdMatcherConstructing(context)
                if (index == context.matcherIndex) {
                    context.matcherIndex++
                }
            } else {
                context.matcherIndex++
            }
        }
    }

    override fun isMatched(target: SegmentId): Boolean {
        val context = ListSegmentIdMatchingContext(target, matchers)
        while (context.matcherIndex < matchers.size && context.targetIndex < target.size) {
            when (val matcher = matchers[context.matcherIndex]) {
                is ListSegmentIdMatcherMatchingCallbackSupport -> {
                    if (!matcher.onListSegmentIdMatcherMatching(context)) {
                        return false
                    }
                    if (context.result != null) {
                        return context.result!!
                    }
                }

                else -> {
                    if (!matcher.isMatched(target[context.targetIndex])) {
                        return false
                    }
                    context.matcherIndex++
                    context.targetIndex++
                }
            }
        }

        // If matcher is not finished, but target is finished.
        // Test if remaining is optional.
        while (context.matcherIndex < matchers.size) {
            val matcherIndex = context.matcherIndex
            val matcher = matchers[context.matcherIndex]
            if (matcher is ListSegmentIdMatcherMatchingCallbackSupport) {
                if (!matcher.onListSegmentIdMatcherMatchingRemaining(context)) {
                    return false
                }
                if (context.result != null) {
                    return context.result!!
                }
                if (context.matcherIndex == matcherIndex) {
                    context.matcherIndex++
                }
            } else {
                return false
            }
        }

        // If target is not finished, but matcher is finished.
        // Try to use the last matcher to match the all.
        if (context.targetIndex < target.size) {
            val lastMatcher = matchers[context.matcherIndex - 1]
            if (lastMatcher is ListSegmentIdMatcherMatchingCallbackSupport) {
                while (context.targetIndex < target.size) {
                    val targetIndex = context.targetIndex

                    if (!lastMatcher.onListSegmentIdMatcherMatchingRemaining(context)) {
                        return false
                    }
                    if (context.result != null) {
                        return context.result!!
                    }

                    if (targetIndex == context.targetIndex) {
                        context.targetIndex++
                    }
                }
                return true
            } else {
                return false
            }
        }

        return true
    }

    private val toStringCache: String by lazy {
        "DefaultStringListMatcher(" +
                "type=$SEGMENT_ID_MATCHER_TYPE_LIST," +
                "segmentMatchers=$matchers)"
    }

    override fun toString(): String = toStringCache

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ListSegmentIdMatcherImpl

        return matchers == other.matchers
    }

    override fun hashCode(): Int {
        return matchers.hashCode()
    }
}
