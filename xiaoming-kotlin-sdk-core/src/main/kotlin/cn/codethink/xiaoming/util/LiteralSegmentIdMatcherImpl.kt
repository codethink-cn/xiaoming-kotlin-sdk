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

const val SEGMENT_ID_MATCHER_TYPE_LITERAL = "segment_id.literal"

@JsonTypeName(SEGMENT_ID_MATCHER_TYPE_LITERAL)
class LiteralSegmentIdMatcherImpl(
    override val value: SegmentId
) : SegmentIdMatcher, LiteralMatcher<SegmentId> {
    private val type: String = SEGMENT_ID_MATCHER_TYPE_LITERAL

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LiteralSegmentIdMatcherImpl

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    private val toStringCache: String by lazy {
        "LiteralStringListMatcher(" +
                "type=$SEGMENT_ID_MATCHER_TYPE_LITERAL," +
                "list=$value)"
    }

    override fun toString(): String = toStringCache
}