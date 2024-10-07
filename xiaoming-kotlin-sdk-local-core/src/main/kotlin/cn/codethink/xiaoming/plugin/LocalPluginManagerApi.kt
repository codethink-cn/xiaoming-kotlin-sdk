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

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.DefaultIdMapRegistrations
import cn.codethink.xiaoming.common.ErrorPolicy
import cn.codethink.xiaoming.common.Expected
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.NamespaceId
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.expectedError
import cn.codethink.xiaoming.common.failure
import cn.codethink.xiaoming.common.success
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

typealias PluginToDetector = Pair<Plugin, Map.Entry<Id, Registration<PluginDetector>>>

/**
 * Manages plugins installed in local platform.
 *
 * @author Chuanwise
 */
class LocalPluginManagerApi(
    val internalApi: LocalPlatformInternalApi
) {
    private val logger: KLogger = KotlinLogging.logger { }

    /**
     * Lock to ensure state switching is safe.
     */
    private val lock = ReentrantReadWriteLock()

    /**
     * API state.
     */
    private enum class State {
        /**
         * API is never started in this platform.
         */
        ALLOCATED,

        /**
         * API is starting.
         */
        STARTING,

        /**
         * API tried to start, but an error occurred.
         */
        STARTING_ERRORED,

        /**
         * API is started.
         */
        STARTED,

        /**
         * API is stopping.
         */
        STOPPING,

        /**
         * API tried to stop, but an error occurred.
         */
        STOPPING_ERRORED,

        /**
         * API is stopped.
         */
        STOPPED
    }

    private var stateNoLock: State = State.ALLOCATED

    /**
     * Plugins allocated to this platform, associated by real ID.
     */
    private val mutPlugins: MutableMap<NamespaceId, AllocatedPlugin> = HashMap()

    /**
     * External view of [mutPlugins].
     */
    val plugins: Map<NamespaceId, AllocatedPlugin>
        get() = mutPlugins.toMap()

    /**
     * Provided plugins, see [PluginMeta.provisions].
     */
    private var mutProvidedPluginIds: MutableMap<NamespaceId, NamespaceId> = HashMap()

    /**
     * Plugin detectors, to detect available installed plugins.
     */
    private val mutDetectors = DefaultIdMapRegistrations<PluginDetector>()

    /**
     * External view of [mutDetectors].
     */
    val detectors: Map<Id, Registration<PluginDetector>>
        get() = mutDetectors.toMap()


    /**
     * Start plugin manager API.
     *
     * If the API is already started, it will be ignored.
     *
     * @param cause the cause of starting.
     * @param force whether to force start the errored plugin.
     * @param policy the policy for expected errors.
     */
    @InternalApi
    fun start(
        cause: Cause,
        force: Boolean = false,
        resolver: PluginCoexistenceResolver = NoOperationPluginCoexistenceResolver,
        policy: ErrorPolicy = ErrorPolicy.THROW_EXCEPTION
    ): Expected<Unit> = lock.write {
        stateNoLock = when (stateNoLock) {
            State.ALLOCATED, State.STOPPED -> State.STARTING
            State.STARTED -> return@write success()
            State.STARTING_ERRORED -> if (force) State.STARTING else {
                return@write failure(logger.expectedError(cause, internalApi.descriptor, policy) {
                    "Plugin manager API is already errored in the last starting attempt. " +
                            "To retry forcefully, set argument `force` to true."
                })
            }

            State.STOPPING_ERRORED -> return@write failure(logger.expectedError(cause, internalApi.descriptor, policy) {
                "Unexpected plugin manager API state: $stateNoLock before starting."
            })

            else -> error("Unexpected plugin manager API state: $stateNoLock before starting.")
        }
        logger.debug { "Starting plugin manager API." }

        try {
            // 1. Detect all plugins.
            val detectedPluginList = detectPlugins(cause)

            // 2. Resolve plugins.
            val resolvedPlugins = resolver.resolve(
                context = PluginConflictResolverContext(
                    platform = internalApi.platform,
                    cause = cause,
                    plugins = detectedPluginList,
                    policy = policy
                )
            )

            stateNoLock = State.STARTED
            return@write success()
        } catch (t: Throwable) {
            stateNoLock = State.STARTING_ERRORED
            throw t
        }
    }

    // Just detect plugins, without id conflict checking.
    private fun detectPlugins(
        cause: Cause
    ): List<PluginToDetector> = lock.write {
        val results = mutableListOf<PluginToDetector>()

        // Get registered plugin detectors and detect all plugins.
        for (entry in mutDetectors) {
            val (id, registration) = entry
            logger.trace { "Detecting plugins by detector '$id' registered by ${registration.subject}." }

            // 1. Call `detectAll`.
            val detected = try {
                registration.value.detectAll(internalApi.platform, cause).toList()
            } catch (t: Throwable) {
                logger.error(t) {
                    "Error occurred when detecting plugins by detector '$id' " +
                            "registered by ${registration.subject}."
                }
                continue
            }

            // 2. Display trace info.
            if (detected.isEmpty()) {
                logger.trace { "No plugin detected by '$id'." }
            } else {
                logger.trace { "Detected: " }
                detected.forEach { logger.trace { " - $it" } }
            }

            // 3. Check if conflict plugin detected.
            results += detected.map { it to entry }
        }

        return@write results
    }

    /**
     * Load multiple plugins. If argument contains a plugin that is already loaded or enabled,
     * it will be ignored. If it's errored, it will be reloaded forcefully if [force] = true,
     * otherwise exception will be thrown because it's not safe to reload an errored plugin.
     *
     * If no error occurred, it's expected that [AllocatedPlugin.isLoaded] return true,
     * otherwise the [AllocatedPlugin.isErrored] should return true.
     *
     * @param plugins the plugins to load.
     * @param force whether to force load the errored plugin.
     * @param cause the cause of this operation.
     *
     * @see AllocatedPlugin.load
     * @see AllocatedPlugin.isLoaded
     */
    fun loadPlugins(
        plugins: Iterable<Plugin>, cause: Cause, force: Boolean = false
    ): Unit = lock.write {
        val pluginsById = plugins.associateBy { it.descriptor.id }

        val alreadyLoadedPluginIds = mutableSetOf<NamespaceId>()

        internalApi.logger.info { "I can't waiting for load plugins!" }
        pluginsById.values.forEach {
            (it as AllocatedPlugin).load(internalApi.platform, cause)
        }
    }

    fun enablePlugins(
        plugins: Iterable<Plugin>,
        cause: Cause,
        force: Boolean = false,
    ): Unit = lock.write {
        val pluginsById = plugins.associateBy { it.descriptor.id }

        val alreadyEnabledPluginIds = mutableSetOf<NamespaceId>()

        // Collate provisions.
        // This -> By Whom (include This)
        val providedPluginIds = HashMap(mutProvidedPluginIds)
        for (required in plugins) {
            val providerId = providedPluginIds[required.descriptor.id]
            if (providerId != null) {
                val provider = mutPlugins[providerId]
                checkNotNull(provider) { "The provider of plugin '${required.id}': '${providerId}' is not found." }

                if (providerId == required.descriptor.id) {
                    // Plugin already provided by itself.
                    if (required.version == provider.version) {
                        // If the version is same, check if it is enabled.
                        if (provider is AllocatedPlugin && provider.isEnabled) {
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
            val presented = mutPlugins[plugin.descriptor.id]
            if (presented != null) {
                // Check if version changed.
                if (presented.version != plugin.version) {
                    // If version changed, we need to unload the previous one.
                    // Then load and enable the new one.

                    if (presented is AllocatedPlugin && presented.isErrored) {
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
        for (presentPlugin in this.mutPlugins) {
            nodes[presentPlugin.key] = Node(presentPlugin.value, true)
        }
    }
}