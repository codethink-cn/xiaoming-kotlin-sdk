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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionMatcher

/**
 * 插件静态元数据。
 *
 * @author Chuanwise
 */
interface PluginMeta {
    /**
     * 插件类型。
     */
    val type: String

    /**
     * 插件 ID。
     */
    val id: NamespaceId

    /**
     * 插件名称，用于显示。
     */
    val name: String

    /**
     * 插件版本。
     */
    val version: Version

    /**
     * 插件的更新频道。
     */
    val channel: String

    /**
     * 插件描述。
     */
    val description: String?

    /**
     * 插件所需的小明标准版本。
     */
    val xiaoming: VersionMatcher?

    /**
     * 插件能够提供的功能。
     */
    val provisions: List<PluginRequirement>

    /**
     * 插件的依赖。
     */
    val dependencies: List<PluginRequirement>

    /**
     * 插件的类别。
     */
    val categories: List<NamespaceId>
}

