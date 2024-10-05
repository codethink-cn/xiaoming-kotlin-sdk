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

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.currentThreadCauseOrFail
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class LocalPluginManagerApi(
    val internalApi: LocalPlatformInternalApi
) {
    val lock = ReentrantReadWriteLock()

    private val mutablePlugins: MutableMap<Id, Plugin> = HashMap()
    val plugins: Map<Id, Plugin>
        get() = mutablePlugins.toMap()

    @InternalApi
    internal fun start(cause: Cause, subject: SubjectDescriptor) {

    }

    fun enablePlugins(
        plugins: Iterable<Plugin>,
        force: Boolean = false,
        cause: Cause? = null
    ): Unit = lock.write {
        val finalCause = cause ?: currentThreadCauseOrFail()
        val pluginsById = plugins.associateBy { it.descriptor.id }

        // Build dependency graph.
        class Node(
            val plugin: Plugin,
            val present: Boolean,

            // Other -> This
            val edgeHeads: MutableMap<Id, Node> = mutableMapOf(),

            // This -> Other
            val edgeTails: MutableMap<Id, Node> = mutableMapOf()
        )

        val nodes = mutableMapOf<Id, Node>()

        // 1. Check if there is any plugin that is in error state.
        for (plugin in plugins) {
            val presented = mutablePlugins[plugin.descriptor.id]
            if (presented != null) {
                // Check if version changed.
                if (presented.version != plugin.version) {
                    // If version changed, we need to unload the previous one.
                    // Then load and enable the new one.

                    if (presented.isErrored) {
                        check(force) {
                            "The plugins to enabled contains '${plugin.toExactRequirement()}', " +
                                    "but there is a same plugin with a another version '${presented.version}', " +
                                    "platform needs to unload it first. " +
                                    "But it is already in an error state and the platform dare not forcefully operate it. " +
                                    "If you really need to enable the new, please set the `force` to true " +
                                    "or disable the presented forcefully first."
                        }
                    }
                }
            }
        }


        // 1. Add present plugins.
        for (presentPlugin in this.mutablePlugins) {
            nodes[presentPlugin.key] = Node(presentPlugin.value, true)
        }
    }
}