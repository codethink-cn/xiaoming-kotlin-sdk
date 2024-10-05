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
import cn.codethink.xiaoming.common.NamespaceId
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

    private var mutableProvidedPluginIds: MutableMap<NamespaceId, NamespaceId> = HashMap()

    @InternalApi
    fun start(cause: Cause, subject: SubjectDescriptor) {

    }

    /**
     * Load multiple plugins. If argument contains a plugin that is already loaded or enabled,
     * it will be ignored. If it's errored, it will be reloaded forcefully if [force] = true,
     * otherwise exception will be thrown because it's not safe to reload an errored plugin.
     *
     * If no error occurred, it's expected that [InitializedPlugin.isLoaded] return true,
     * otherwise the [InitializedPlugin.isErrored] should return true.
     *
     * @param plugins the plugins to load.
     * @param force whether to force load the errored plugin.
     * @param cause the cause of this operation.
     *
     * @see InitializedPlugin.load
     * @see InitializedPlugin.isLoaded
     */
    fun loadPlugins(
        plugins: Iterable<Plugin>, force: Boolean = false, cause: Cause? = null
    ): Unit = lock.write {
        val finalCause = cause ?: currentThreadCauseOrFail()
        val pluginsById = plugins.associateBy { it.descriptor.id }

        val alreadyLoadedPluginIds = mutableSetOf<NamespaceId>()

    }

    fun enablePlugins(
        plugins: Iterable<Plugin>,
        force: Boolean = false,
        cause: Cause? = null
    ): Unit = lock.write {
        val finalCause = cause ?: currentThreadCauseOrFail()
        val pluginsById = plugins.associateBy { it.descriptor.id }

        val alreadyEnabledPluginIds = mutableSetOf<NamespaceId>()

        // Collate provisions.
        // This -> By Whom (include This)
        val providedPluginIds = HashMap(mutableProvidedPluginIds)
        for (required in plugins) {
            val providerId = providedPluginIds[required.descriptor.id]
            if (providerId != null) {
                val provider = mutablePlugins[providerId]
                checkNotNull(provider) { "The provider of plugin '${required.id}': '${providerId}' is not found." }

                if (providerId == required.descriptor.id) {
                    // Plugin already provided by itself.
                    if (required.version == provider.version) {
                        // If the version is same, check if it is enabled.
                        if (provider is InitializedPlugin && provider.isEnabled) {
                            // Plugin already enabled.
                            alreadyEnabledPluginIds.add(required.descriptor.id)
                            continue
                        }
                    }
                }

                // Plugin already provided by another plugin.
            }
        }

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

                    if (presented is InitializedPlugin && presented.isErrored) {
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