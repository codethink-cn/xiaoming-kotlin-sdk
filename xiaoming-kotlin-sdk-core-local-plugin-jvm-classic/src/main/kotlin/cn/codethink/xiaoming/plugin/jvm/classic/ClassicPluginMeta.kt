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

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.common.NamespaceId
import cn.codethink.xiaoming.common.Version
import cn.codethink.xiaoming.common.VersionMatcher
import cn.codethink.xiaoming.plugin.OptionalPluginRequirement
import cn.codethink.xiaoming.plugin.PluginMeta
import com.fasterxml.jackson.annotation.JsonTypeName

const val PLUGIN_META_TYPE_CLASSIC = "jvm.classic"

/**
 * Represent a file in classic plugin resource file `plugin.yml`.
 *
 * @param logger The logger name of this plugin.
 * @param main The main class of this plugin.
 * @author Chuanwise
 */
@JsonTypeName(PLUGIN_META_TYPE_CLASSIC)
class ClassicPluginMeta(
    override val id: NamespaceId,
    override val name: String,
    override val version: Version,
    override val channel: String,
    val main: String,
    val logger: String? = null,
    override val dependencies: List<OptionalPluginRequirement> = emptyList(),
    override val description: String? = null,
    override val protocol: VersionMatcher? = null,
    override val provisions: List<OptionalPluginRequirement> = emptyList()
) : PluginMeta {
    override val type: String = PLUGIN_META_TYPE_CLASSIC
}