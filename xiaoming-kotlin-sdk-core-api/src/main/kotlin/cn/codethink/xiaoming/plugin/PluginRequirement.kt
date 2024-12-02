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

@file:JvmName("PluginRequirements")

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.StringMatcher
import cn.codethink.xiaoming.util.VersionMatcher
import cn.codethink.xiaoming.util.createPluginRequirement
import cn.codethink.xiaoming.util.parsePluginRequirement

/**
 * 插件需求。
 *
 * @author Chuanwise
 * @see parsePluginRequirement
 * @see createPluginRequirement
 */
interface PluginRequirement {
    val id: NamespaceId
    val version: VersionMatcher?
    val channel: StringMatcher?
    val optional: Boolean
    val local: Boolean
}

fun String.toPluginRequirement(): PluginRequirement = parsePluginRequirement(this)