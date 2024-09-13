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

@file:JvmName("Ids")

package cn.codethink.xiaoming.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * Mark a class's instance as an id. All its implementation classes must be
 * serializable and deserializable, and implements the [equals], [toString]
 * and [hashCode].
 *
 * @author Chuanwise
 */
interface Id

const val SEGMENT_SEPARATOR = "."
const val SEGMENT_REGEXP_STRING = "[\\w-]+"

val SEGMENT_REGEXP = SEGMENT_REGEXP_STRING.toRegex()

const val SEGMENT_ID_REGEXP_STRING = "[\\w-]+(\\.[\\w-]+)*"
val SEGMENT_ID_REGEXP = SEGMENT_ID_REGEXP_STRING.toRegex()

/**
 * A segment id is a string that contains several segments (not empty),
 * separated by a dot. Each segment should match the [SEGMENT_REGEXP_STRING].
 *
 * For example, `a.b.c` is a valid segment id, while `a..b` is not.
 *
 * @author Chuanwise
 */
@JsonSerialize(using = SegmentIdStringSerializer::class)
@JsonDeserialize(using = SegmentIdStringDeserializer::class)
data class SegmentId(
    private val segments: List<String>
) : Id, List<String> by segments {
    init {
        assert(segments.isNotEmpty()) { "Segments should not be empty." }
        assert(segments.all { it.matches(SEGMENT_REGEXP) }) { "Segments should match the regexp: $SEGMENT_REGEXP_STRING." }
    }

    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String): SegmentId = segmentIdOf(string)
    }

    fun toList(): List<String> = segments

    private val toStringCache: String = segments.joinToString(SEGMENT_SEPARATOR)
    override fun toString(): String = toStringCache

    override fun hashCode(): Int = toStringCache.hashCode()
    override fun equals(other: Any?): Boolean = other is SegmentId && other.toString() == toString()
}

fun segmentIdOf(string: String): SegmentId = SegmentId(string.split("."))
fun String.toSegmentId(): SegmentId = segmentIdOf(this)

object SegmentIdStringSerializer : StdSerializer<SegmentId>(SegmentId::class.java) {
    private fun readResolve(): Any = SegmentIdStringSerializer
    override fun serialize(segmentId: SegmentId, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(segmentId.toString())
    }
}

object SegmentIdStringDeserializer : StdDeserializer<SegmentId>(SegmentId::class.java) {
    private fun readResolve(): Any = SegmentIdStringDeserializer
    override fun deserialize(
        parser: com.fasterxml.jackson.core.JsonParser,
        context: com.fasterxml.jackson.databind.DeserializationContext
    ): SegmentId {
        return segmentIdOf(parser.text)
    }
}
