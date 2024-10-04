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

import cn.codethink.xiaoming.common.AnyVersionMatcher
import cn.codethink.xiaoming.common.JavaFriendlyApi
import cn.codethink.xiaoming.common.NamespaceId
import cn.codethink.xiaoming.common.Requirement
import cn.codethink.xiaoming.common.VersionMatcher
import cn.codethink.xiaoming.common.toSegmentId
import cn.codethink.xiaoming.common.toVersionMatcher
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * Describe plugin requirement.
 *
 * @author Chuanwise
 */
@JsonSerialize(using = PluginRequirementSerializer::class)
@JsonDeserialize(using = PluginRequirementDeserializer::class)
class PluginRequirement(
    override val id: NamespaceId,
    override val version: VersionMatcher,
    val channel: String? = null
) : Requirement {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String) = string.toPluginRequirement()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluginRequirement

        if (id != other.id) return false
        if (version != other.version) return false
        if (channel != other.channel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + (channel?.hashCode() ?: 0)
        return result
    }

    private val toStringCache: String = buildString {
        append(id)
        append(':')
        append(version)
        if (channel != null) {
            append('@')
            append(channel)
        }
    }

    override fun toString(): String = toStringCache
}

object PluginRequirementSerializer : StdSerializer<PluginRequirement>(PluginRequirement::class.java) {
    private fun readResolve(): Any = PluginRequirementSerializer
    override fun serialize(requirement: PluginRequirement, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(requirement.toString())
    }
}

object PluginRequirementDeserializer : StdDeserializer<PluginRequirement>(PluginRequirement::class.java) {
    private fun readResolve(): Any = PluginRequirementDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): PluginRequirement {
        return parser.valueAsString.toPluginRequirement()
    }
}

/**
 * Parse a string to a [PluginRequirement].
 *
 * Example:
 *
 * ```
 * cn.codethink.xiaoming:lexicons                   // Any version of this plugin is allowed.
 * cn.codethink.xiaoming:lexicons@beta              // Any version, but only beta version is allowed.
 * cn.codethink.xiaoming:lexicons@beta              // Any version, but only beta version is allowed. And it's optional.
 * cn.codethink.xiaoming:lexicons:1.0.0             // Only version 1.0.0 of this plugin is allowed.
 * cn.codethink.xiaoming:lexicons:1.0.0@release     // Only version 1.0.0 of this plugin is allowed. And it's release.
 * ```
 *
 * BNF patterns:
 *
 * ```bnf
 * requirement := id                                 // Any version and any channel.
 *              | id ":" versionMatcher              // Any matched version and any channel.
 *              | id ":" versionMatcher "@" channel  // Any matched version and specified channel.
 *              | id "@" channel                     // Any version and specified channel.
 *              ;
 * ```
 *
 * @see NamespaceId
 * @see VersionMatcher
 */

fun String.toPluginRequirement(): PluginRequirement {
    require(isNotEmpty()) {
        "Plugin requirement string should not be empty."
    }

    val colonIndexAfterGroup = indexOf(':', 0)
    require(colonIndexAfterGroup != -1) {
        "Plugin dependency string should contain a colon."
    }
    val group = substring(0, colonIndexAfterGroup).toSegmentId()

    val colonIndexAfterName = indexOf(':', colonIndexAfterGroup + 1)
    val name = if (colonIndexAfterName == -1) {
        substring(colonIndexAfterGroup + 1)
    } else {
        substring(colonIndexAfterGroup + 1, colonIndexAfterName)
    }

    val pluginId = NamespaceId(group, name)

    val atIndex = lastIndexOf('@', length)
    val channel = if (atIndex == -1) {
        null
    } else {
        substring(atIndex + 1, length)
    }

    val atIndexOrLength = if (atIndex == -1) length else atIndex
    val versionMatcher = if (atIndexOrLength == colonIndexAfterName + 1) {
        AnyVersionMatcher
    } else {
        substring(colonIndexAfterName + 1, atIndexOrLength).toVersionMatcher()
    }

    return PluginRequirement(pluginId, versionMatcher, channel)
}