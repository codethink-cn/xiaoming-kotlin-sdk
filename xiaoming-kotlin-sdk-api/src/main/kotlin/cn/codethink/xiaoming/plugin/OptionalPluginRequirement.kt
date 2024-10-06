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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.common.JavaFriendlyApi
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
 * @see toOptionalPluginRequirement
 */
@JsonSerialize(using = OptionalPluginRequirementSerializer::class)
@JsonDeserialize(using = OptionalPluginRequirementDeserializer::class)
class OptionalPluginRequirement(
    val requirement: PluginRequirement,
    val optional: Boolean = false
) {
    val id by requirement::id
    val version by requirement::version
    val channel by requirement::channel

    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String) = string.toOptionalPluginRequirement()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OptionalPluginRequirement

        if (id != other.id) return false
        if (version != other.version) return false
        if (optional != other.optional) return false

        return true
    }

    private val hashCodeCache: Int = run {
        var result = id.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + optional.hashCode()
        result
    }

    override fun hashCode() = hashCodeCache

    private val toStringCache: String = buildString {
        append(requirement)
        if (optional) {
            append("?")
        }
    }

    override fun toString(): String = toStringCache
}

object OptionalPluginRequirementSerializer :
    StdSerializer<OptionalPluginRequirement>(OptionalPluginRequirement::class.java) {
    private fun readResolve(): Any = OptionalPluginRequirementSerializer
    override fun serialize(
        dependency: OptionalPluginRequirement,
        generator: JsonGenerator,
        provider: SerializerProvider
    ) {
        generator.writeString(dependency.toString())
    }
}

object OptionalPluginRequirementDeserializer :
    StdDeserializer<OptionalPluginRequirement>(OptionalPluginRequirement::class.java) {
    private fun readResolve(): Any = OptionalPluginRequirementDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): OptionalPluginRequirement {
        return parser.valueAsString.toOptionalPluginRequirement()
    }
}

/**
 * Parse a string to a [OptionalPluginRequirement].
 */
fun String.toOptionalPluginRequirement(): OptionalPluginRequirement {
    val optional = endsWith("?")
    val requirement = substringBeforeLast("?").toPluginRequirement()
    return OptionalPluginRequirement(requirement, optional)
}