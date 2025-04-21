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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize(using = SegmentIdMatcherImplSerializer::class)
@JsonDeserialize(using = SegmentIdMatcherImplDeserializer::class)
data class SegmentIdMatcherImpl(
    private val matchers: List<StringMatcher>
) : SegmentIdMatcher {
    init {
        if (matchers.isEmpty()) {
            throw IllegalArgumentException("Segment matchers should not be empty.")
        }
        for (index in matchers.indices) {
            val matcher = matchers[index]
            if (matcher is WildCardStringMatcher) {
                if (index > 0) {
                    // If it is not the first one, check if previous one is `WildcardStringMatcher`.
                    val previous = matchers[index - 1]
                    if (previous == AnyStringMatcher) {
                        throw IllegalArgumentException(
                            "AnyMatcher<String> next to WildcardStringMatcher near index $index!"
                        )
                    }
                }
                if (index < matchers.size - 1) {
                    // If it is not the last one, check if next one is `WildcardStringMatcher` or any optional matcher.
                    val next = matchers[index + 1]
                    if (next is WildCardStringMatcher || next !== AnyStringMatcher) {
                        throw IllegalArgumentException(
                            "WildcardStringMatcher appear continuously near index $index!"
                        )
                    }
                }
            }
        }
    }

    override fun matches(segmentId: SegmentId): Boolean {
        var matcherIndex = 0
        var targetIndex = 0

        while (matcherIndex < matchers.size && targetIndex < segmentId.size) {
            when (val matcher = matchers[matcherIndex]) {
                is WildCardStringMatcher -> {
                    // If current matcher is the last one, and the element is the last one, matched.
                    if (matcherIndex == matchers.size - 1) {
                        TODO("Not implemented yet.")
                    }

                    matcherIndex++
                    val nextMatcher = matchers[matcherIndex]

                    // The matcher after WildcardStringMatcher must be functional.
                    check(nextMatcher !is WildCardStringMatcher) { "WildcardStringMatcher can not followed by another WildcardStringMatcher or AnyStringMatcher." }

                    // Or get the next matcher and find matched segment.
                    var nextTargetIndex: Int
                    if (matcher.majority) {
                        // Majority.
                        nextTargetIndex = segmentId.size - 1
                        while (nextTargetIndex >= targetIndex) {
                            val nextSegment = segmentId[nextTargetIndex]
                            if (nextMatcher.matches(nextSegment)) {
                                break
                            }
                            nextTargetIndex--
                        }
                        if ((nextTargetIndex < targetIndex && matcher.optional) ||
                            (nextTargetIndex <= targetIndex && !matcher.optional)
                        ) {
                            return false
                        }
                        targetIndex = nextTargetIndex + 1
                        matcherIndex++
                    } else {
                        // Minority.
                        nextTargetIndex = targetIndex
                        while (nextTargetIndex < segmentId.size) {
                            val nextSegment = segmentId[nextTargetIndex]
                            if (nextMatcher.matches(nextSegment)) {
                                break
                            }
                            nextTargetIndex++
                        }
                        if (nextTargetIndex == targetIndex && !matcher.optional) {
                            return false
                        }
                        targetIndex = nextTargetIndex
                    }
                }

                else -> {
                    if (!matcher.matches(segmentId[targetIndex])) {
                        return false
                    }
                    matcherIndex++
                    targetIndex++
                }
            }
        }

        // If matcher is not finished, but target is finished.
        // Test if remaining is optional.
        while (matcherIndex < matchers.size) {
            val matcher = matchers[matcherIndex]
            if (matcher is WildCardStringMatcher && matcher.optional) {
                matcherIndex++
            } else {
                // If it is not optional, return false.
                return false
            }
        }

        // If target is not finished, but matcher is finished.
        // Try to use the last matcher to match the all.
        if (targetIndex < segmentId.size) {
            TODO("Not implemented yet.")
        }

        return true
    }

    override fun toString(): String = matchers.joinToString("")
}

object SegmentIdMatcherImplSerializer : JsonSerializer<SegmentIdMatcherImpl>() {
    override fun serialize(value: SegmentIdMatcherImpl, gen: JsonGenerator, serializers: SerializerProvider) {
        return gen.writeString(value.toString())
    }
}

object SegmentIdMatcherImplDeserializer : JsonDeserializer<SegmentIdMatcherImpl>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): SegmentIdMatcherImpl {
        return parser.valueAsString.toSegmentIdMatcher() as SegmentIdMatcherImpl
    }
}