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

@file:JvmName("Plugins")

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.PluginSubjectDescriptor
import cn.codethink.xiaoming.util.Subject
import cn.codethink.xiaoming.util.Version

/**
 * 插件是一些功能的集合，以对平台产生影响。
 *
 * @author Chuanwise
 * @see NotYetAllocatedPlugin
 * @see AllocatedPlugin
 */
sealed interface Plugin : Subject {
    /**
     * 插件描述符。
     */
    override val descriptor: PluginSubjectDescriptor

    /**
     * 插件元数据。
     */
    val meta: PluginMeta

    /**
     * 插件源。
     */
    val source: PluginSource
}

val Plugin.id: NamespaceId
    get() = meta.id

val Plugin.name: String
    get() = meta.name

val Plugin.version: Version
    get() = meta.version

fun Plugin.toPluginRequirement() = meta.toPluginRequirement()
