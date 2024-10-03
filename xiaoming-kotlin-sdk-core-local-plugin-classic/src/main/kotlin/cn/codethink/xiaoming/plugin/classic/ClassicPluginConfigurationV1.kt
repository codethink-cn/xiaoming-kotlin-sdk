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

package cn.codethink.xiaoming.plugin.classic

import cn.codethink.xiaoming.plugin.PluginLevel
import com.fasterxml.jackson.annotation.JsonTypeName

const val CLASSIC_PLUGIN_CONFIGURATION_VERSION = "1"

@JsonTypeName(CLASSIC_PLUGIN_CONFIGURATION_VERSION)
class ClassicPluginConfigurationV1(
    override val distribution: String,
    override val level: PluginLevel = PluginLevel.ENABLE,
    override val autoUpdate: Boolean = true,
) : ClassicPluginConfiguration {
    override val version: String = CLASSIC_PLUGIN_CONFIGURATION_VERSION
}