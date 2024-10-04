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
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * Namespace Id.
 *
 * @author Chuanwise
 */
@JsonSerialize(using = NamespaceIdSerializer::class)
@JsonDeserialize(using = NamespaceIdDeserializer::class)
class NamespaceId(
    val group: SegmentId,
    val name: String
) : Id {
    init {
        assert(name.matches(SEGMENT_REGEX)) { "Name should match the regexp: $SEGMENT_REGEX." }
    }

    private val toStringCache = "$group:$name"

    override fun toString(): String = toStringCache

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NamespaceId

        if (group != other.group) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = toStringCache.hashCode()
}

const val NAMESPACE_ID_MATCHER_TYPE = "namespace_id"

@JsonTypeName(NAMESPACE_ID_MATCHER_TYPE)
class LiteralNamespaceIdMatcher(
    override val value: NamespaceId
) : LiteralMatcher<NamespaceId> {
    override val type: String = NAMESPACE_ID_MATCHER_TYPE

    override val targetType: Class<NamespaceId>
        get() = NamespaceId::class.java

    override val targetNullable: Boolean
        get() = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LiteralNamespaceIdMatcher

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "LiteralNamespaceIdMatcher(value=$value)"
    }
}

fun NamespaceId.toLiteralMatcher() = LiteralNamespaceIdMatcher(this)

fun String.toNamespaceId(): NamespaceId {
    val split = split(':')
    assert(split.size == 2) { "Namespace Id should be split by ':'" }
    return NamespaceId(split[0].toSegmentId(), split[1])
}

fun String.toNamespaceIdOrNull(): NamespaceId? {
    try {
        return toNamespaceId()
    } catch (_: Exception) {
    }
    return null
}

fun parseNamespaceId(string: String) = string.toNamespaceId()

object NamespaceIdSerializer : StdSerializer<NamespaceId>(NamespaceId::class.java) {
    private fun readResolve(): Any = NamespaceIdSerializer
    override fun serialize(value: NamespaceId, generator: JsonGenerator, provider: SerializerProvider) {
        return generator.writeString(value.toString())
    }
}

object NamespaceIdDeserializer : StdDeserializer<NamespaceId>(NamespaceId::class.java) {
    private fun readResolve(): Any = NamespaceIdDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): NamespaceId {
        return parser.valueAsString.toNamespaceId()
    }
}