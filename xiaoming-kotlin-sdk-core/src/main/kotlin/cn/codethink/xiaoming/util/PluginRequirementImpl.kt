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

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.plugin.PluginRequirement
import cn.codethink.xiaoming.plugin.toPluginRequirement
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.StringMatcher
import cn.codethink.xiaoming.util.VersionMatcher
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * Describe plugin dependency or provisions.
 *
 * @author Chuanwise
 * @see toOptionalPluginDemand
 */
@JsonSerialize(using = PluginDemandSerializer::class)
@JsonDeserialize(using = PluginDemandDeserializer::class)
class PluginRequirementImpl(
    override val id: NamespaceId,
    override val version: VersionMatcher?,
    override val channel: StringMatcher?,
    override val optional: Boolean,
    override val local: Boolean
) : PluginRequirement {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluginRequirementImpl

        if (id != other.id) return false
        if (version != other.version) return false
        if (channel != other.channel) return false
        if (optional != other.optional) return false
        if (local != other.local) return false

        return true
    }

    private val hashCodeCache: Int = run {
        var result = id.hashCode()
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + (channel?.hashCode() ?: 0)
        result = 31 * result + optional.hashCode()
        result = 31 * result + local.hashCode()
        result
    }

    override fun hashCode() = hashCodeCache

    private val toStringCache: String = buildString {
        append(id)
        append(':')
        append(version)
        if (channel != null) {
            append('@')
            append(channel)
        }
        if (optional) {
            append("?")
        }
        if (local) {
            append('!')
        }
    }

    override fun toString(): String = toStringCache
}

object PluginDemandSerializer : StdSerializer<PluginRequirementImpl>(PluginRequirementImpl::class.java) {
    private fun readResolve(): Any = PluginDemandSerializer
    override fun serialize(
        dependency: PluginRequirementImpl,
        generator: JsonGenerator,
        provider: SerializerProvider
    ) {
        generator.writeString(dependency.toString())
    }
}

object PluginDemandDeserializer : StdDeserializer<PluginRequirementImpl>(PluginRequirementImpl::class.java) {
    private fun readResolve(): Any = PluginDemandDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): PluginRequirementImpl {
        return parser.valueAsString.toPluginRequirement() as PluginRequirementImpl
    }
}
