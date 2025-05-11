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
 * cn.codethink.xiaoming:lexicons:1.0.0!            // 版本为 1.0.0 的 stable 频道的 lexicons 插件，且必须是这个插件而非其他插件提供的功能。
 * ```
 *
 * BNF 范式如下：
 *
 * ```bnf
 * pattern := id versionPatternOrAny originalOrAny requiredOrAny;
 *
 * versionPatternOrAny :=                           // 任意版本。
 *                     | ":" versionPattern         // 指定范围的版本。
 *                     ;

 * originalOrAny :=                                 // 任意模式。
 *               | "!"                              // 必须由原始插件提供。
 *               ;
 *
 * requiredOrAny :=                                 // 任意模式。
 *               | "?"                              // 可选插件。
 *               ;
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
    required: Boolean = true,
    original: Boolean = false
): PluginDependency {
    return CoreApi.getInstance().createPluginDependency(id, version, required, original)
}

@JvmSynthetic
fun PluginMeta.toPluginDependency(required: Boolean = true, original: Boolean = false): PluginDependency {
    return PluginDependency(
        id = id,
        version = version.toLiteralVersionPattern(),
        required = required,
        original = original
    )
}

@JvmSynthetic
fun String.toPluginDependency(): PluginDependency {
    return PluginDependency(this)
}