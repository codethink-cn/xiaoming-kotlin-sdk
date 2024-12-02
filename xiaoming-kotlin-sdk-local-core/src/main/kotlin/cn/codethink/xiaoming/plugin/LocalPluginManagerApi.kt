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

import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.IdMapRegistrations
import cn.codethink.xiaoming.util.RegistrationImpl
import cn.codethink.xiaoming.util.DualKeyMap
import cn.codethink.xiaoming.util.MutableDualKeyMap
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Registration
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

data class DetectedPlugin(
    val plugin: Plugin,
    val detector: PluginDetector,
    val detectorId: Id,
    val detectorRegistration: Registration<PluginDetector>
)

/**
 * Internal context of a plugin including allocated flag, lifecycle methods.
 *
 * Notice that that lifecycle methods' duty is to:
 *
 * 1. Trigger corresponding events.
 * 2. Maintain flags in [PluginRuntimeMeta].
 * 3. Call the corresponding lifecycle methods of the plugin.
 * 4. Handle exceptions.
 *
 * @author Chuanwise
 */
interface PluginInternalContext {
    val key: DualKeyMap.Key<NamespaceId, Version>
    val plugin: Plugin

    val isAllocated: Boolean
    val isAllocating: Boolean
    val isAllocatingErrored: Boolean

    fun allocate(cause: Cause, force: Boolean = false): AllocatedPlugin

    fun load(platform: Platform, cause: Cause, force: Boolean = false)
    fun enable(platform: Platform, cause: Cause, force: Boolean = false)
    fun disable(platform: Platform, cause: Cause, force: Boolean = false)
    fun unload(platform: Platform, cause: Cause, force: Boolean = false)
}

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

    // Plugin container can storage multi versions of a plugin.
    private inner class PluginInternalContextImpl(
        override val key: DualKeyMap.Key<NamespaceId, Version>,
        var pluginNoLock: Plugin,
        val runtimeMeta: MutablePluginRuntimeMeta
    ) : PluginInternalContext {
        val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

        override val plugin: Plugin
            get() = lock.read { pluginNoLock }

        override var isAllocated: Boolean = false
        override var isAllocating: Boolean = false
        override var isAllocatingErrored: Boolean = false

        /**
         * Allocate plugin if not, or return the allocated plugin.
         */
        override fun allocate(cause: Cause, force: Boolean): AllocatedPlugin = lock.write {
            val currentPlugin = plugin
            if (currentPlugin is AllocatedPlugin) {
                return currentPlugin
            }
            check(!isAllocatingErrored || force) {
                "The plugin '${plugin.toPluginRequirement()}' is errored when allocating, " +
                        "and the platform dare not forcefully operate it. " +
                        "If you really need to allocate it, please set the `force` to true and try again."
            }

            val notYetAllocatedPlugin = pluginNoLock
            check(notYetAllocatedPlugin is NotYetAllocatedPlugin<*>) {
                "The plugin '${pluginNoLock.toPluginRequirement()}' is not a NotYetAllocatedPlugin."
            }

            isAllocating = true
            try {
                // Do allocate operation.
                val context = PluginAllocatingContext(internalApi.platform, cause, runtimeMeta)
                val allocated = notYetAllocatedPlugin.allocate(context)

                // Only if allocate success, then return the allocated plugin.
                isAllocated = true
                isAllocatingErrored = false

                pluginNoLock = allocated

                return@write allocated
            } catch (t: Throwable) {
                isAllocatingErrored = true
                throw t
            } finally {
                isAllocating = false
            }
        }

        override fun load(platform: Platform, cause: Cause, force: Boolean): Unit = lock.write {
            val plugin = allocate(cause, force)

            if (runtimeMeta.isLoaded) {
                return
            }
            require(!runtimeMeta.isLoadingErrored || force) {
                "The plugin '${plugin.toPluginRequirement()}' is errored when loading, " +
                        "and the platform dare not forcefully operate it. " +
                        "If you really need to load it, please set the `force` to true and try again."
            }

            // Check if only one version of the plugin is loaded.
            val versions = pluginInternalContexts.toMapByKey1(key.key1)

            val versionsLoadingOrAttempted = versions.filterValues {
                it !== this && (it.runtimeMeta.isLoading || it.runtimeMeta.isLoadingAttempted)
            }
            check(versionsLoadingOrAttempted.isEmpty()) {
                "Other "
            }

            // TODO: Trigger event.
            val event = PluginLoadEvent

            runtimeMeta.isLoading = true
            try {
                plugin.load(platform, cause, force)
                runtimeMeta.isLoaded = true
                runtimeMeta.isLoadingErrored = false
            } catch (t: Throwable) {
                runtimeMeta.isLoadingErrored = true
                throw t
            } finally {
                runtimeMeta.isLoading = false
            }
        }

        override fun enable(platform: Platform, cause: Cause, force: Boolean) {
            TODO("Not yet implemented")
        }

        override fun disable(platform: Platform, cause: Cause, force: Boolean) {
            TODO("Not yet implemented")
        }

        override fun unload(platform: Platform, cause: Cause, force: Boolean) {
            TODO("Not yet implemented")
        }
    }

    private val pluginInternalContexts: MutableDualKeyMap<NamespaceId, Version, PluginInternalContextImpl> =
        MutableDualKeyMap()

    /**
     * External view of [pluginInternalContexts].
     */
    val plugins: Collection<Plugin>
        get() = pluginInternalContexts.toValues().map { it.pluginNoLock }

    /**
     * Provided plugins, see [PluginMeta.provisions].
     */
    private var mutableProvidedPluginIds: MutableMap<NamespaceId, NamespaceId> = HashMap()

    /**
     * Plugin detectors, to detect available installed plugins.
     */
    private val mutablePluginDetectors = IdMapRegistrations<PluginDetector>()

    /**
     * External view of [mutablePluginDetectors].
     */
    val detectors: Map<Id, Registration<PluginDetector>>
        get() = mutablePluginDetectors.toMap()

    // Just detect plugins, without id conflict checking.
    fun detectPlugins(
        cause: Cause
    ): List<DetectedPlugin> = lock.write {
        val results = mutableListOf<DetectedPlugin>()

        // Get registered plugin detectors and detect all plugins.
        for (entry in mutablePluginDetectors) {
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
            results += detected.map {
                DetectedPlugin(
                    plugin = it,
                    detector = registration.value,
                    detectorId = id,
                    detectorRegistration = registration
                )
            }
        }

        return@write results
    }

    private fun putAndCheckNotInternalContextRegistered(
        key: DualKeyMap.Key<NamespaceId, Version>,
        internalContext: PluginInternalContextImpl
    ) {
        val oldInternalContext = pluginInternalContexts.putIfAbsent(key, internalContext)
        if (oldInternalContext != null) {
            val requirement = oldInternalContext.plugin.toPluginRequirement()
            error("The plugin '$requirement' is already registered.")
        }
    }

    /**
     * Allocate a plugin. If the plugin is already allocated, an exception will be thrown.
     *
     * Notice that the method will NOT check if dependencies or other conditions are
     * satisfied, it's the caller's responsibility.
     *
     * If failed, the plugin will not be added to the [pluginInternalContexts].
     *
     * @param plugin the plugin to register, must be a [NotYetAllocatedPlugin].
     * @return the allocated plugin.
     *
     * @see NotYetAllocatedLocalPlugin
     * @see NotYetAllocatedRemotePlugin
     */
    @InternalApi
    fun registerPlugin(plugin: NotYetAllocatedPlugin): PluginInternalContext {
        val runtimeMeta: MutablePluginRuntimeMeta = when (plugin) {
            is NotYetAllocatedRemotePlugin -> RemotePluginRuntimeMetaRemoteViewImpl()
            is NotYetAllocatedLocalPlugin -> LocalPluginRuntimeMetaImpl()
            else -> error(
                "Unexpected plugin type: ${plugin::class.qualifiedName}, " +
                        "required 'cn.codethink.xiaoming.plugin.NotYetAllocatedRemotePlugin' or " +
                        "'cn.codethink.xiaoming.plugin.NotYetAllocatedLocalPlugin'."
            )
        }

        val key = DualKeyMap.Key(plugin.id, plugin.version)
        val internalContext = PluginInternalContextImpl(key, plugin, runtimeMeta)

        putAndCheckNotInternalContextRegistered(key, internalContext)
        return internalContext
    }

    fun getPluginInternalContext(key: DualKeyMap.Key<NamespaceId, Version>): PluginInternalContext? = lock.read {
        pluginInternalContexts[key]
    }

    @InternalApi
    fun unloadPluginsNoCheck(
        plugins: Map<NamespaceId, AllocatedPlugin>, cause: Cause, force: Boolean
    ): Unit = lock.write {
        // Plugins are not empty, and values are in the `mutablePlugins`.

        // 1. Disable plugins if needed.
        val presentedPluginsToDisableBeforeActions = mutableMapOf<NamespaceId, AllocatedPlugin>()
        for ((id, plugin) in pluginInternalContexts) {
            if (plugin.isEnabled) {
                presentedPluginsToDisableBeforeActions[id] = plugin
            } else if (plugin.runtimeMeta.isDisablingErrored) {
                check(force) {
                    "The plugins to unload contains '${plugin.toExactRequirement()}', " +
                            "but it is disabling errored, platform dare not forcefully operate it. " +
                            "If you really need to unload it, please set the `force` to true and try again."
                }
                presentedPluginsToDisableBeforeActions[id] = plugin
            }
        }

        disablePluginsNoCheck(presentedPluginsToDisableBeforeActions, cause, force)

        // 2. Unload them.
        for ((id, plugin) in pluginInternalContexts) {
            val context = mutablePluginRuntimeMetas[id] ?: error("The plugin context of plugin '$id' is not found.")

            // If plugin already unloaded, ignore it.

        }
    }

    fun registerPluginDetector(
        id: Id, detector: PluginDetector, subject: SubjectDescriptor
    ): Unit = lock.write {
        mutablePluginDetectors.register(id, RegistrationImpl(detector, subject))
    }

    fun unregisterPluginDetectorById(id: Id): Unit = lock.write {
        mutablePluginDetectors.unregisterByKey(id)
    }

    fun unregisterPluginDetectorBySubject(subject: SubjectDescriptor): Unit = lock.write {
        mutablePluginDetectors.unregisterBySubject(subject)
    }
}