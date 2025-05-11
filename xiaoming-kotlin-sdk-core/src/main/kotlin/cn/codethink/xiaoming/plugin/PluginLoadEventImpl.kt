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

import cn.codethink.xiaoming.event.AbstractCancellableEvent
import cn.codethink.xiaoming.event.EventContext
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Time

class PluginLoadEventImpl(
    override val pluginId: NamespaceId,
    override val cause: Cause?,
    override val operator: SubjectDescriptor,
    override val time: Time,
    override val id: Id
) : AbstractCancellableEvent(), PluginLoadEvent {
    override lateinit var plugin: Plugin
    override val description: String = buildString {
        append("Operator $operator is loading plugin $pluginId at $time")
        cause?.let {
            append(" due to ")
            append(it.description)
        }
        append(" with operation id $id")
    }

    override fun applyEventContext(context: EventContext<PluginLoadEvent>) {
        plugin = context.platform.pluginManager.getPluginOrFail(pluginId)
    }
}