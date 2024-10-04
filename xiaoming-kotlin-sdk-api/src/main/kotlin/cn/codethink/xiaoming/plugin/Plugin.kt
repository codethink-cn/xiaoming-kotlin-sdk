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

import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.NamespaceId
import cn.codethink.xiaoming.common.PluginSubjectDescriptor
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.Version

/**
 * Plugin is a set of functions that extend platform's abilities. It can service
 * platform locally and remotely.
 *
 * @author Chuanwise
 */
interface Plugin : Subject {
    val meta: PluginRuntimeMeta
    override val descriptor: PluginSubjectDescriptor

    fun load(platform: Platform, cause: Cause, subject: SubjectDescriptor)
    fun enable(platform: Platform, cause: Cause, subject: SubjectDescriptor)
    fun disable(platform: Platform, cause: Cause, subject: SubjectDescriptor)
    fun unload(platform: Platform, cause: Cause, subject: SubjectDescriptor)
}

val Plugin.id: NamespaceId
    get() = descriptor.id

val Plugin.name: String
    get() = descriptor.id.name

val Plugin.group: SegmentId
    get() = descriptor.id.group

val Plugin.version: Version
    get() = meta.meta.version

val Plugin.isError: Boolean
    get() = meta.isError

val Plugin.isEnabled: Boolean
    get() = meta.isEnabled

val Plugin.isLoaded: Boolean
    get() = meta.isLoaded

val Plugin.isNotError: Boolean
    get() = meta.isNotError

val Plugin.isNotEnabled: Boolean
    get() = meta.isNotEnabled

val Plugin.isNotLoaded: Boolean
    get() = meta.isNotLoaded