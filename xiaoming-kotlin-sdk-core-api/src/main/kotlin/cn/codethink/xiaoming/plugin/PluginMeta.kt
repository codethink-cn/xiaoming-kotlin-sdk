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

import cn.codethink.xiaoming.common.NamespaceId
import cn.codethink.xiaoming.common.Version
import cn.codethink.xiaoming.common.VersionMatcher
import cn.codethink.xiaoming.common.toLiteralVersionMatcher

interface PluginMeta {
    /**
     * Plugin type, such as "classic".
     */
    val type: String

    /**
     * Plugin universal id, such as "cn.codethink:user".
     */
    val id: NamespaceId

    /**
     * Plugin name to display, such as "User".
     */
    val name: String

    /**
     * Plugin version.
     */
    val version: Version

    /**
     * Plugin channel, such as "release".
     */
    val channel: String

    /**
     * A brief description of the plugin.
     */
    val description: String?

    /**
     * Protocol version matcher.
     */
    val protocol: VersionMatcher?

    /**
     * Describes what other plugins' features the plugin can provide.
     *
     * For plugin A, if it can provide plugins B, C? (optional) and D, plugins depended on
     * them can be enabled even if plugin B, C or D is not enabled.
     */
    val provisions: List<OptionalPluginRequirement>

    /**
     * Plugin dependencies.
     */
    val dependencies: List<OptionalPluginRequirement>
}

fun PluginMeta.toExactRequirement() = PluginRequirement(
    id = id, version = version.toLiteralVersionMatcher(), channel = channel
)

@JvmOverloads
fun PluginMeta.toExactDependency(optional: Boolean = false) = OptionalPluginRequirement(
    requirement = toExactRequirement(), optional = optional
)