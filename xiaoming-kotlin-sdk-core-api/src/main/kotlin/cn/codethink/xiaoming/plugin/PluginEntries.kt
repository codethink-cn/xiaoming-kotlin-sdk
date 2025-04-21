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

@file:JvmName("PluginEntries")

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Version

val PluginEntry.id: NamespaceId get() = meta.id
val PluginEntry.name: String get() = meta.name
val PluginEntry.version: Version get() = meta.version

fun PluginEntry.toPluginRequirement() = meta.toPluginRequirement()

val PluginEntry.isRemotePlugin: Boolean get() = mode == PluginMode.REMOTE
val PluginEntry.isLocalPlugin: Boolean get() = mode == PluginMode.LOCAL
