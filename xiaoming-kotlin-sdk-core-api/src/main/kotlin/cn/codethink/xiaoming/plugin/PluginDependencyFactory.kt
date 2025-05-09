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

@file:JvmName("PluginDependencyFactory")

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.api.CoreApi
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.VersionPattern
import cn.codethink.xiaoming.util.toLiteralVersionPattern

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
 * @see VersionPattern
 */
@OptIn(InternalApi::class)
@JvmName("createPluginDependency")
fun PluginDependency(string: String): PluginDependency {
    return CoreApi.getInstance().createPluginDependency(string)
}

@JvmOverloads
@OptIn(InternalApi::class)
@JvmName("createPluginDependency")
fun PluginDependency(
    id: NamespaceId,
    version: VersionPattern? = null,
    required: Boolean = true
): PluginDependency {
    return CoreApi.getInstance().createPluginDependency(id, version, required)
}

@JvmSynthetic
fun PluginMeta.toPluginDependency(required: Boolean = true): PluginDependency {
    return PluginDependency(
        id = id,
        version = version.toLiteralVersionPattern(),
        required = required
    )
}

@JvmSynthetic
fun String.toPluginDependency(): PluginDependency {
    return PluginDependency(this)
}