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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.common.NamespaceId
import cn.codethink.xiaoming.common.PluginSubjectDescriptor
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.Version

/**
 * Plugin is a set of functions that extend platform's abilities. It can service
 * platform locally and remotely.
 *
 * @author Chuanwise
 * @see NotYetAllocatedPlugin
 * @see AllocatedPlugin
 */
sealed interface Plugin : Subject {
    /**
     * Plugin's descriptor.
     */
    override val descriptor: PluginSubjectDescriptor

    /**
     * Plugin's meta information.
     */
    val meta: PluginMeta

    /**
     * Plugin's source.
     */
    val source: PluginSource
}

val Plugin.id: NamespaceId
    get() = descriptor.id

val Plugin.name: String
    get() = descriptor.id.name

val Plugin.group: SegmentId
    get() = descriptor.id.group

val Plugin.version: Version
    get() = meta.version

fun Plugin.toExactRequirement() = meta.toExactRequirement()

@JvmOverloads
fun Plugin.toExactDependency(optional: Boolean = false) = meta.toExactDependency(optional)