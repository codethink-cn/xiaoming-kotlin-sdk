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
 * Describe plugin dependency.
 *
 * @author Chuanwise
 * @see toPluginDependency
 */
@JsonSerialize(using = PluginDependencySerializer::class)
@JsonDeserialize(using = PluginDependencyDeserializer::class)
class PluginDependency(
    val requirement: PluginRequirement,
    val optional: Boolean = false
) {
    val id by requirement::id
    val version by requirement::version
    val channel by requirement::channel

    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String) = string.toPluginDependency()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluginDependency

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

object PluginDependencySerializer : StdSerializer<PluginDependency>(PluginDependency::class.java) {
    private fun readResolve(): Any = PluginDependencySerializer
    override fun serialize(dependency: PluginDependency, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(dependency.toString())
    }
}

object PluginDependencyDeserializer : StdDeserializer<PluginDependency>(PluginDependency::class.java) {
    private fun readResolve(): Any = PluginDependencyDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): PluginDependency {
        return parser.valueAsString.toPluginDependency()
    }
}

/**
 * Parse a string to a [PluginDependency].
 */
fun String.toPluginDependency(): PluginDependency {
    val optional = endsWith("?")
    val requirement = substringBeforeLast("?").toPluginRequirement()
    return PluginDependency(requirement, optional)
}