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

@file:JvmName("PluginRequirementFactory")

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.api.CoreApi
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.VersionMatcher
import cn.codethink.xiaoming.util.toLiteralVersionMatcher

/**
 * 编译字符串为对应插件需求。
 *
 * 示例：
 *
 * ```
 * cn.codethink.xiaoming:lexicons                   // 任意版本的 lexicons 插件均可符合需求。
 * cn.codethink.xiaoming:lexicons:1.0.0             // 版本为 1.0.0 的 lexicons 插件。
 * cn.codethink.xiaoming:lexicons@stable            // 任意版本的 stable 频道的 lexicons 插件。
 * cn.codethink.xiaoming:lexicons:1.0.0@stable      // 版本为 1.0.0 的 stable 频道的 lexicons 插件。
 * cn.codethink.xiaoming:lexicons:1.0.0@stable!     // 版本为 1.0.0 的 stable 频道的 lexicons 插件，且必须是本地插件。
 * ```
 *
 * BNF 范式如下：
 *
 * ```bnf
 * requirement := id versionMatcherOrAny channelOrAny modeOrAny;
 *
 * versionMatcherOrAny :=                           // 任意版本。
 *                     | ":" versionMatcher         // 指定范围的版本。
 *                     ;
 *
 * channelOrAny :=                                  // 任意频道。
 *              | "@" channel                       // 指定频道。
 *              ;
 *
 * modeOrAny :=                                     // 任意模式。
 *           | "!";                                 // 本地模式。
 *           ;
 * ```
 *
 * @see NamespaceId
 * @see VersionMatcher
 */
@OptIn(InternalApi::class)
@JvmName("createPluginRequirement")
fun PluginRequirement(string: String): PluginRequirement {
    return CoreApi.getInstance().createPluginRequirement(string)
}

@JvmOverloads
@OptIn(InternalApi::class)
@JvmName("createPluginRequirement")
fun PluginRequirement(
    id: NamespaceId,
    version: VersionMatcher? = null,
    optional: Boolean = false,
    local: Boolean = false
): PluginRequirement {
    return CoreApi.getInstance().createPluginRequirement(id, version, optional, local)
}

@JvmSynthetic
fun PluginMeta.toPluginRequirement(optional: Boolean = false, local: Boolean = false): PluginRequirement {
    return PluginRequirement(
        id = id,
        version = version.toLiteralVersionMatcher(),
        optional = optional,
        local = local
    )
}

@JvmSynthetic
fun String.toPluginRequirement(): PluginRequirement {
    return PluginRequirement(this)
}