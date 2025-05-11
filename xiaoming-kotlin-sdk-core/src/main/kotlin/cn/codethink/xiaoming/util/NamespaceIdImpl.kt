/*
 * Copyright 2025 CodeThink Technologies and contributors.
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

@JsonSerialize(using = NamespaceIdSerializer::class)
@JsonDeserialize(using = NamespaceIdDeserializer::class)
class NamespaceIdImpl(
    override val group: SegmentId,
    override val name: String
) : NamespaceId {
    init {
        check(SegmentIdImpl.SEGMENT_REGEX.matches(name)) { "Name should match the regexp: ${SegmentIdImpl.SEGMENT_REGEX}." }
    }

    private val toStringCache = "$group:$name"

    override fun toString(): String = toStringCache

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NamespaceId

        if (group != other.group) return false
        return name == other.name
    }

    override fun hashCode(): Int = toStringCache.hashCode()
}

object NamespaceIdSerializer : StdSerializer<NamespaceIdImpl>(NamespaceIdImpl::class.java) {
    private fun readResolve(): Any = NamespaceIdSerializer
    override fun serialize(value: NamespaceIdImpl, generator: JsonGenerator, provider: SerializerProvider) {
        return generator.writeString(value.toString())
    }
}

object NamespaceIdDeserializer : StdDeserializer<NamespaceIdImpl>(NamespaceIdImpl::class.java) {
    private fun readResolve(): Any = NamespaceIdDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): NamespaceIdImpl {
        return parser.valueAsString.toNamespaceId() as NamespaceIdImpl
    }
}