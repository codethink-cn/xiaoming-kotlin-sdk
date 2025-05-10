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

@file:JvmName("PluginPatternFactory")

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.api.CoreApi
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.VersionPattern
import cn.codethink.xiaoming.util.toLiteralVersionPattern

@JvmOverloads
@OptIn(InternalApi::class)
@JvmName("createPluginPattern")
fun PluginPattern(id: NamespaceId, version: VersionPattern? = null): PluginPattern {
    return CoreApi.getInstance().createPluginPattern(id, version)
}

@OptIn(InternalApi::class)
@JvmName("createPluginPattern")
fun PluginPattern(string: String): PluginPattern {
    return CoreApi.getInstance().createPluginPattern(string)
}

@JvmSynthetic
fun String.toPluginPattern(): PluginPattern {
    return PluginPattern(this)
}

@JvmSynthetic
fun PluginMeta.toPluginPattern(): PluginPattern {
    return PluginPattern(id, version.toLiteralVersionPattern())
}