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
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

private const val SEGMENT_SEPARATOR = "."
private val SEGMENT_REGEX = "[\\w-]+".toRegex()

@JsonSerialize(using = SegmentIdStringSerializer::class)
@JsonDeserialize(using = SegmentIdStringDeserializer::class)
data class SegmentIdImpl(
    private val segments: List<String>
) : SegmentId, Id, List<String> by segments {
    init {
        assert(segments.isNotEmpty()) { "Segments should not be empty." }
        assert(segments.all { it.matches(SEGMENT_REGEX) }) { "Segments should match the regexp: $SEGMENT_REGEX." }
    }

    override fun toList(): List<String> = segments

    private val toStringCache: String = segments.joinToString(SEGMENT_SEPARATOR)
    override fun toString(): String = toStringCache

    override fun hashCode(): Int = toStringCache.hashCode()
    override fun equals(other: Any?): Boolean = other is SegmentIdImpl && other.toString() == toString()
}

object SegmentIdStringSerializer : StdSerializer<SegmentIdImpl>(SegmentIdImpl::class.java) {
    private fun readResolve(): Any = SegmentIdStringSerializer
    override fun serialize(segmentId: SegmentIdImpl, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(segmentId.toString())
    }
}

object SegmentIdStringDeserializer : StdDeserializer<SegmentIdImpl>(SegmentIdImpl::class.java) {
    private fun readResolve(): Any = SegmentIdStringDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): SegmentIdImpl {
        return parser.valueAsString.toSegmentId() as SegmentIdImpl
    }
}