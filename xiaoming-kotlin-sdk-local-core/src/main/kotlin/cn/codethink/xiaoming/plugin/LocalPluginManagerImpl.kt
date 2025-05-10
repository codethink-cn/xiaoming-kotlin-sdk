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

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.RemotePlatform
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.DAG
import cn.codethink.xiaoming.util.ExperimentalApi
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MutableDualKeyMap
import cn.codethink.xiaoming.util.MutableDualKeyMapImpl
import cn.codethink.xiaoming.util.MutableMapRegistrationManagerImpl
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.Registration
import cn.codethink.xiaoming.util.SdkConstants
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.productSize
import cn.codethink.xiaoming.util.products
import cn.codethink.xiaoming.util.runIf
import cn.codethink.xiaoming.util.runIfNotNull
import cn.codethink.xiaoming.util.toMutableDualKeyMap
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.reflect.jvm.jvmName

@OptIn(InternalApi::class, ExperimentalApi::class)
class LocalPluginManagerImpl(
    override val platform: LocalPlatform
) : LocalPluginManager {
    private val logger: KLogger = KotlinLogging.logger(PluginManager::class.jvmName)

    // 具有相同 ID，但用不同版本的插件可以并存，但是只有一个可以加载。
    private var mutableAvailablePlugins = MutableDualKeyMapImpl<NamespaceId, Version, AbstractLocalServingPlugin>()
    override val availablePlugins: Collection<Plugin> get() = mutableAvailablePlugins.values.toList()

    // 插件一旦加载，便需先将自己置于其中。以免相同 ID，不同版本的插件被同时加载。
    private var mutablePlugins = ConcurrentHashMap<NamespaceId, Plugin>()
    override val plugins: Map<NamespaceId, Plugin> get() = mutablePlugins.toMap()

    private val mutablePluginScanners = MutableMapRegistrationManagerImpl<String, PluginScanner>()
    override val pluginScanners: Map<String, Registration<PluginScanner>> get() = mutablePluginScanners.toRegistrationMap()

    private val mutablePluginSources = MutableMapRegistrationManagerImpl<String, PluginSource>()
    override val pluginSources: Map<String, Registration<PluginSource>> get() = mutablePluginSources.toRegistrationMap()

    // 尝试为本插件获取 LOAD 锁。返回持有独占当前 ID 启动权的插件。若为 this 表示获取成功。
    private fun tryAcquireUniquePluginLock(plugin: Plugin): Plugin {
        return mutablePlugins.computeIfAbsent(plugin.id) { plugin }
    }

    private fun acquireUniquePluginLock(plugin: Plugin) {
        val oldPlugin = tryAcquireUniquePluginLock(plugin)
        check(oldPlugin === plugin) {
            "There is another plugin ${oldPlugin.meta.id} with version ${oldPlugin.version} is already loaded. " +
                    "Note that multiple plugins with the same ID are not allowed to be loaded at the same time. " +
                    "Please unload the old one first."
        }
    }

    // 插件被成功 UNLOAD，释放独占当前 ID 的锁。若返回 null 表示成功（也可能从未获取），否则失败。
    private fun tryReleaseUniquePluginLock(plugin: Plugin): Plugin? {
        return mutablePlugins.computeIfPresent(plugin.id) { _, nowPlugin ->
            if (nowPlugin === plugin) {
                null
            } else {
                nowPlugin
            }
        }
    }

    private fun releaseUniquePluginLock(plugin: Plugin) {
        val nowPlugin = tryReleaseUniquePluginLock(plugin)
        check(nowPlugin == null) {
            "Plugin ${plugin.meta.id} is not loaded and its version is ${nowPlugin?.version}. " +
                    "Fail to release unique plugin lock for another version ${plugin.version}."
        }
    }

    private val onLocalServingPluginLoad: Consumer<AbstractPlugin> = Consumer { acquireUniquePluginLock(it) }
    private val onLocalServingPluginUnloadedOrCrashed: Consumer<AbstractPlugin> = Consumer { releaseUniquePluginLock(it) }
    private val onLocalPluginReleased: Consumer<AbstractPlugin> = Consumer { mutableAvailablePlugins.remove(it.id, it.version) }

    private abstract inner class AbstractLocalServingPlugin(
        meta: PluginMeta, val configuration: PluginConfiguration, handler: PluginHandler, operation: Operation
    ) : AbstractPlugin(
        meta, handler, this, operation, PluginState.UNALLOCATED, platform,
        onLocalServingPluginLoad, onLocalServingPluginUnloadedOrCrashed, onLocalPluginReleased
    )

    private inner class SharablePluginImpl(
        meta: PluginMeta, configuration: PluginConfiguration, operation: Operation, handler: PluginHandler
    ) : AbstractLocalServingPlugin(meta, configuration, handler, operation), SharablePlugin {
        private val mutableInstances: MutableMap<RemotePlatform, RemoteServingPlugin> = ConcurrentHashMap()
        override val instances: Map<RemotePlatform, RemoteServingPlugin> get() = mutableInstances.toMap()

        private val onRemoteServingPluginLoad = Consumer<AbstractPlugin> { }
        private val onRemoteServingPluginUnloadedOrCrashed = Consumer<AbstractPlugin> { }
        private val onRemotePluginReleased = Consumer<AbstractPlugin> { mutableInstances.remove(it.platform) }

        private inner class RemoteServingPluginImpl(
            meta: PluginMeta, operation: Operation, handler: PluginHandler, platform: RemotePlatform
        ) : AbstractPlugin(
            meta, handler, this@LocalPluginManagerImpl, operation, PluginState.ALLOCATED, platform,
            onRemoteServingPluginLoad, onRemoteServingPluginUnloadedOrCrashed, onRemotePluginReleased
        ), RemoteServingPlugin {
            override val backend: SharablePlugin get() = this@SharablePluginImpl
        }

        override fun registerInstance(platform: RemotePlatform, handler: PluginHandler, operation: Operation): RemoteServingPlugin {
            val newPlugin = RemoteServingPluginImpl(meta, operation, handler, platform)
            val oldPlugin = mutableInstances.computeIfAbsent(platform) { newPlugin }
            require(oldPlugin === newPlugin) { "Plugin ${meta.id} is already registered" }
            return newPlugin
        }
    }

    private inner class PluginImpl(
        meta: PluginMeta, configuration: PluginConfiguration, operation: Operation, handler: PluginHandler
    ) : AbstractLocalServingPlugin(meta, configuration, handler, operation)

    private fun createPlugin(meta: PluginMeta, configuration: PluginConfiguration, operation: Operation, handler: PluginHandler): AbstractLocalServingPlugin {
        return when (configuration.sharable) {
            true -> SharablePluginImpl(meta, configuration, operation, handler)
            false -> PluginImpl(meta, configuration, operation, handler)
        }
    }

    override fun registerPlugin(meta: PluginMeta, configuration: PluginConfiguration, handler: PluginHandler, operation: Operation): Plugin {
        val newPlugin = createPlugin(meta, configuration, operation, handler)
        val oldPlugin = mutableAvailablePlugins.putIfAbsent(meta.id, meta.version, newPlugin)
        require(oldPlugin == null) { "Plugin ${meta.id} is already registered" }
        return newPlugin
    }

    override fun getPlugin(namespaceId: NamespaceId): Plugin? {
        return mutablePlugins[namespaceId]
    }

    override fun getAvailablePlugin(id: NamespaceId, version: Version): Plugin? {
        return mutableAvailablePlugins[id, version]
    }

    override fun getAvailablePlugins(id: NamespaceId): Map<Version, Plugin> {
        return mutableAvailablePlugins[id]
    }

    override fun getAvailablePlugins(pattern: PluginPattern): Map<Version, Plugin> {
        return getAvailablePlugins(pattern.id).runIfNotNull(pattern.version) { p -> filterKeys { p.matches(it) } }
    }

    private inner class PluginScanContextImpl(override val operation: Operation) : PluginScanContext {
        val newAvailablePlugins: MutableDualKeyMapImpl<NamespaceId, Version, AbstractLocalServingPlugin> = MutableDualKeyMapImpl()

        override fun registerPlugin(meta: PluginMeta, configuration: PluginConfiguration, operation: Operation, handler: PluginHandler) {
            val newPlugin = createPlugin(meta, configuration, operation, handler)
            val oldPlugin = newAvailablePlugins.putIfAbsent(meta.id, meta.version, newPlugin)
            require(oldPlugin == null) { "Plugin ${meta.id} is already registered" }
        }
    }

    override suspend fun resolvePlugins(pattern: PluginPattern, operation: Operation): Map<Version, Plugin> {
        flushAvailablePlugins(operation)
        getProviderPluginsFromSources(pattern, operation)
        return getAvailablePlugins(pattern)
    }

    override suspend fun flushAvailablePlugins(operation: Operation) {
        val pluginScanContext = PluginScanContextImpl(operation)
        for (registration in mutablePluginScanners.registrations) {
            registration.value.scan(pluginScanContext)
        }

        val oldAvailablePlugins = mutableAvailablePlugins
        val newAvailablePlugins = pluginScanContext.newAvailablePlugins

        val removedPlugins = oldAvailablePlugins.filterKeys { !newAvailablePlugins.containsKey(it) }
        for (removedPlugin in removedPlugins) {
            if (removedPlugin.value.configuration.crashOnRemoved) {
                removedPlugin.value.ensureCrashed(operation)
            }
        }
    }

    private suspend fun getProviderPluginsFromSources(pattern: PluginPattern, cause: Cause): Map<String, List<PluginEntry>> {
        val results = mutableMapOf<String, List<PluginEntry>>()
        for (registration in mutablePluginSources.registrations) {
            logger.trace {
                "Getting providable plugins from plugin source: ${registration.key} " +
                        "(plugin source registered due to ${registration.operation.description})"
            }

            val pluginAvailableVersions = try {
                registration.value.getProviderPlugins(pattern, cause)
            } catch (t: Throwable) {
                logger.warn(t) {
                    "Fail to get providable plugins for $pattern from plugin source: ${registration.key} " +
                            "(plugin source registered due to ${registration.operation.description}, operation due to ${cause.description})."
                }
                continue
            }

            val sourceResults = mutableListOf<PluginEntry>()
            for (entry in pluginAvailableVersions) {
                // 检查插件所需的小明协议最低版本是否符合要求。
                val standardPattern = entry.meta.standard
                if (standardPattern != null && !standardPattern.matches(SdkConstants.STANDARD_VERSION)) {
                    logger.trace {
                        "Plugin ${entry.meta.id} is not compatible with current SDK (SDK version: ${SdkConstants.SDK_VERSION_STRING}). " +
                                "The highest standard version supported by the SDK is ${SdkConstants.STANDARD_VERSION}, " +
                                "but the plugin required standard version pattern is $standardPattern."
                    }
                    continue
                }

                sourceResults.add(entry)
            }
            results[registration.key] = sourceResults
        }
        return results
    }

    sealed interface AvailablePluginEntry {
        val plugin: AbstractPlugin
    }

    // 原本就存在于系统中的插件 Entry
    class OriginalPluginEntry(
        override val plugin: AbstractPlugin
    ) : AvailablePluginEntry

    // 将会被安装的插件 Entry
    class InstallingPluginEntry(
        override val plugin: AbstractPlugin,
        val pluginSourceKey: String
    ) : AvailablePluginEntry

    private class PluginDependencyGraph {
        class PluginNode(val id: NamespaceId) {
            val versionNodes: MutableMap<Version, PluginVersionNode> = mutableMapOf()

            fun getOrCreateVersionNode(version: Version): PluginVersionNode {
                return versionNodes.computeIfAbsent(version) { PluginVersionNode(this, it) }
            }
        }

        class PluginVersionNode(
            val pluginNode: PluginNode,
            val version: Version
        ) {
            // 谁可以提供这个版本？包括这个插件本身。
            val providerNodes: MutableList<AvailablePluginNode> = mutableListOf()
        }

        class AvailablePluginNode(
            val pluginVersionNode: PluginVersionNode,
            val pluginEntry: AvailablePluginEntry
        ) {
            // 这个插件依赖于哪些其他的插件？
            val dependencyNodes: MutableList<AvailablePluginDependencyNode> = mutableListOf()

            // 这个插件的依赖关系有可能被满足吗（true 不一定就能满足，但 false 一定不能满足）
            var mayDependencySatisfied = true

            // 这个插件提供了哪些其他插件？
            val providableVersionNodes: MutableList<PluginVersionNode> = mutableListOf()
        }

        class AvailablePluginDependencyNode(
            val pluginNode: PluginNode,
            val dependency: PluginDependency
        ) {
            // 这个依赖需求可以被哪些插件的哪些版本满足？
            val providableVersionNodes: MutableList<AvailablePluginNode> = mutableListOf()
        }

        val pluginNodes: MutableMap<NamespaceId, PluginNode> = mutableMapOf()

        val availablePluginNodesByKey: MutableDualKeyMap<NamespaceId, Version, AvailablePluginNode> = MutableDualKeyMapImpl()
        val availablePluginNodesByPlugin: MutableMap<AbstractPlugin, AvailablePluginNode> = mutableMapOf()

        // 可以提供某个插件某个版本的所有插件（包含它自己）的节点。
        val providablePluginVersionNodes: MutableDualKeyMap<NamespaceId, Version, MutableList<AvailablePluginNode>> = MutableDualKeyMapImpl()

        fun getOrCreatePluginNode(id: NamespaceId): PluginNode {
            return pluginNodes.computeIfAbsent(id) { PluginNode(id) }
        }

        fun getPluginNode(id: NamespaceId): PluginNode? {
            return pluginNodes[id]
        }

        fun getOrCreateAvailablePluginNode(pluginVersionNode: PluginVersionNode, entry: AvailablePluginEntry): AvailablePluginNode {
            val plugin = entry.plugin
            var value = availablePluginNodesByPlugin[plugin]
            if (value == null) {
                value = AvailablePluginNode(pluginVersionNode, entry)

                availablePluginNodesByKey[plugin.id, plugin.version] = value
                availablePluginNodesByPlugin[plugin] = value
            }
            return value
        }

        fun appendAvailablePlugins(availablePlugins: Map<Pair<NamespaceId, Version>, AvailablePluginEntry>) {
            val additionalAvailablePlugins = availablePlugins.filterKeys { !availablePluginNodesByKey.containsKey(it) }

            // 构造节点并连接提供关系。
            for (entry in additionalAvailablePlugins) {
                val id = entry.key.first
                val version = entry.key.second

                val pluginNode = getOrCreatePluginNode(id)
                val versionNode = pluginNode.getOrCreateVersionNode(version)

                val availablePluginNode = getOrCreateAvailablePluginNode(versionNode, entry.value)

                // 通过 provisions 列表连接插件之间的提供关系。
                for (provision in availablePluginNode.pluginEntry.plugin.meta.provisions) {
                    // 查找当前插件能够提供的其他插件的版本节点。
                    // 此处使用 getOrCreate 的原因是被提供的插件可能本来并不存在。
                    val providablePluginNode = getOrCreatePluginNode(provision.id)

                    val provisionVersionPattern = provision.version
                    val provisionVersionNodes = providablePluginNode
                        .versionNodes
                        .values
                        .runIfNotNull(provisionVersionPattern) { pattern -> filter { pattern.matches(it.version) } }

                    for (provisionVersionNode in provisionVersionNodes) {
                        provisionVersionNode.providerNodes.add(availablePluginNode)
                        availablePluginNode.providableVersionNodes.add(provisionVersionNode)
                    }
                }

                // 连接插件和它自己的提供关系。
                versionNode.providerNodes.add(availablePluginNode)
                availablePluginNode.providableVersionNodes.add(versionNode)

                // 添加提供列表。
                for (providableVersion in availablePluginNode.providableVersionNodes) {
                    providablePluginVersionNodes.computeIfAbsent(
                        providableVersion.pluginNode.id, providableVersion.version
                    ) { mutableListOf() }.add(availablePluginNode)
                }
            }

            // 构造依赖关系。
            for (entry in additionalAvailablePlugins) {
                val id = entry.key.first
                val version = entry.key.second

                val pluginNode = getOrCreatePluginNode(id)
                val versionNode = pluginNode.getOrCreateVersionNode(version)

                val availablePluginNode = getOrCreateAvailablePluginNode(versionNode, entry.value)

                for (dependency in availablePluginNode.pluginEntry.plugin.meta.dependencies) {
                    val dependencyNode = AvailablePluginDependencyNode(pluginNode, dependency)
                    availablePluginNode.dependencyNodes.add(dependencyNode)

                    val dependencyVersionPattern = dependency.version

                    // 找到当前插件依赖的上游插件节点，此时该插件应当已被提供。
                    val dependencyPluginVersionNodes = providablePluginVersionNodes[dependency.id]
                        .runIfNotNull(dependencyVersionPattern) { pattern -> filterKeys { pattern.matches(it) } }
                        .flatMap { it.value }
                        .runIf(dependency.original) { filter { it.pluginEntry.plugin.id == dependency.id } }

                    for (value in dependencyPluginVersionNodes) {
                        dependencyNode.providableVersionNodes.add(value)
                    }
                }
            }
        }
    }

    override suspend fun loadPlugins(operation: Operation) {
        val availablePluginEntries: MutableDualKeyMap<NamespaceId, Version, AvailablePluginEntry> = mutableAvailablePlugins
            .mapValues { OriginalPluginEntry(it.value) }
            .toMutableDualKeyMap()

        doActivatePlugins(availablePluginEntries, operation) {

        }
    }

    private suspend fun doActivatePlugins(
        availablePluginEntries: MutableDualKeyMap<NamespaceId, Version, AvailablePluginEntry>,
        operation: Operation, block: suspend (AbstractLocalServingPlugin) -> Unit
    ) {
        // 对于每个插件，我们不停地获取它们的所有版本的可提供插件，并递归地获取它们的依赖插件，直到 availablePlugins 不再增加为止。
        var newAvailablePluginEntries: Collection<AvailablePluginEntry> = availablePluginEntries.values
        do {
            val nextNewAvailablePluginEntries = mutableListOf<AvailablePluginEntry>()
            for (availablePluginEntry in newAvailablePluginEntries) {
                for (dependency in availablePluginEntry.plugin.meta.dependencies) {
                    val cause = Cause(
                        description = "Fetch plugin ${availablePluginEntry.plugin.meta.id} dependency: $dependency",
                        cause = operation
                    )

                    val providerPluginsFromSources = getProviderPluginsFromSources(dependency, cause)
                    for ((key, entries) in providerPluginsFromSources) {
                        for (entry in entries) {
                            val meta = entry.meta
                            val handler = try {
                                entry.toPluginHandler()
                            } catch (t: Throwable) {
                                logger.warn(t) {
                                    "Fail to get plugin handler for ${meta.id} from plugin source: $key " +
                                            "(plugin source registered due to ${key}, operation due to ${cause.description})."
                                }
                                continue
                            }

                            val newPlugin = createPlugin(meta, entry.configuration, operation, handler)
                            val newPluginEntry = InstallingPluginEntry(newPlugin, key)

                            val nowPluginEntry = availablePluginEntries.compute(Pair(meta.id, meta.version)) { _, now ->
                                when (now) {
                                    null -> newPluginEntry
                                    is InstallingPluginEntry -> {
                                        if (now.pluginSourceKey == key) {
                                            error("Plugin source $key returned at least two plugins with the same ID and version: ${meta.id}:${meta.version}.")
                                        } else {
                                            // 如果有多个插件源提供了相同的插件，则只取第一个插件源的结果。
                                            now
                                        }
                                    }

                                    is OriginalPluginEntry -> {
                                        if (now.plugin.isLoaded || now.plugin.isCrashed) {
                                            // 如果插件已经被加载，则不会更新它。
                                            // 如果 CRASHED 且还在表内，可能是有意为之（因为默认情况下会释放）
                                            now
                                        } else {
                                            logger.trace {
                                                "Plugin ${now.plugin.meta.id}:${now.plugin.version} is already registered, " +
                                                        "but it is not loaded and not crashed. It will be replaced by the new plugin from source $key."
                                            }
                                            newPluginEntry
                                        }
                                    }
                                }
                            }

                            // 如果当前插件确实被添加进表，则准备在下一轮迭代时收集它的依赖插件。
                            if (newPluginEntry === nowPluginEntry) {
                                nextNewAvailablePluginEntries.add(newPluginEntry)
                            }
                        }
                    }
                }
            }
            newAvailablePluginEntries = nextNewAvailablePluginEntries
        } while (newAvailablePluginEntries.isNotEmpty())

        // 构造依赖关系图。
        val dependencyGraph = PluginDependencyGraph().apply {
            appendAvailablePlugins(availablePluginEntries)
        }

        // 根据依赖关系能否满足为插件染色。
        // 此检查只检查插件的依赖关系是否有人提供，并不检查依赖插件能否被提供。
        // 此时检查出无法满足，则必定无法满足。但若能满足，也未必其依赖能被提供。
        logger.trace { "Check dependency resolution..." }
        for (availablePluginNode in dependencyGraph.availablePluginNodesByPlugin.values) {
            val dependencyNodeCount = availablePluginNode.dependencyNodes.size
            for (i in 0 until dependencyNodeCount) {
                val dependencyNode = availablePluginNode.dependencyNodes[i]
                if (dependencyNode.providableVersionNodes.isEmpty() && dependencyNode.dependency.required) {
                    val id = availablePluginNode.pluginVersionNode.pluginNode.id
                    val version = availablePluginNode.pluginVersionNode.version

                    logger.trace { "The ${i + 1}-th dependency of plugin $id:$version is not satisfied." }
                    availablePluginNode.mayDependencySatisfied = false
                    break
                }
            }
        }

        // 插件 ID -> 插件版本 -> [ 上述 ID + 版本的所有提供商 ]
        val providablePluginVersionNodes = dependencyGraph.providablePluginVersionNodes

        // 为每个插件选择启动的候选版本。其按照其所能提供的功能的版本号降序排列，排除无法提供的插件。
        val pluginAvailableNodeCandidates = mutableMapOf<NamespaceId, List<Pair<Version, PluginDependencyGraph.AvailablePluginNode>>>()
        for ((pluginId, versionProviders) in providablePluginVersionNodes.firstEntries) {
            val candidatesBeforeFiltering = versionProviders
                .flatMap { it.value.map { e -> it.key to e } }

            val candidates = candidatesBeforeFiltering
                .filter { it.second.mayDependencySatisfied }
                .sortedByDescending { it.first }

            if (candidates.isEmpty()) {
                logger.trace {
                    val beforeFiltering = candidatesBeforeFiltering.joinToString {
                        val plugin = it.second.pluginEntry.plugin
                        "${plugin.id}:${plugin.version}"
                    }
                    "Plugin $pluginId has no available plugin candidates. Before filtering: $beforeFiltering"
                }
                continue
            }
            pluginAvailableNodeCandidates[pluginId] = candidates
        }

        // 插件 ID -> [ 能够提供该插件的可达插件列表，且必定非空 ]
        val pluginAvailableNodeCandidateList = pluginAvailableNodeCandidates.entries.toList()
        val availablePluginNodeCandidates = pluginAvailableNodeCandidateList.map { it.value }

        // 构造笛卡尔积数量个依赖解析方案。
        val solutionCount = availablePluginNodeCandidates.productSize()
        if (solutionCount == 0) {
            logger.trace { "No solution found for plugin dependency resolution." }
            return
        }

        // 尝试每一种方案。
        var solutionIndex = 1
        logger.trace { "Found $solutionCount solutions for plugin dependency resolution." }
        for (availablePluginNodes in availablePluginNodeCandidates.products()) {
            logger.trace {
                val solution = availablePluginNodes.joinToString {
                    val plugin = it.second.pluginEntry.plugin
                    "${plugin.id}:${plugin.version}"
                }
                "Try to resolve plugin dependency with the $solutionIndex / $solutionCount solution: $solution"
            }

            val availablePluginNodeCount = availablePluginNodes.size
            val availablePluginNodeSet = availablePluginNodes.map { it.second }.toSet()

            // 计算插件都是被谁提供的：插件 ID -> ( 插件版本, 提供者 )
            val providers = mutableMapOf<NamespaceId, Pair<Version, PluginDependencyGraph.AvailablePluginNode>>()
//            val provisions = mutableMapOf<>()
            for (i in 0 until availablePluginNodeCount) {
                val pair = availablePluginNodes[i]

                val provisionId = pluginAvailableNodeCandidateList[i].key
                val provisionVersion = pair.first

                providers[provisionId] = pair
            }

            // 计算依赖关系，构建依赖关系遍历图，以便开始激活插件。
            val traversingDependencyGraph = DAG<Pair<Version, PluginDependencyGraph.AvailablePluginNode>>()
            val traversingDependencyGraphNodes = availablePluginNodes.map { traversingDependencyGraph.allocate(it) }

            val availablePluginNodesToIndex = availablePluginNodes.mapIndexed { index, pair -> pair.second.pluginEntry to index }.toMap()
            for (i in 0 until availablePluginNodeCount) {
                val pair = availablePluginNodes[i]

                val dependencyNodes = pair.second.dependencyNodes
                val dependencyNodeCount = dependencyNodes.size

                val traversingDependencyGraphNode = traversingDependencyGraphNodes[i]

                for (j in 0 until dependencyNodeCount) {
                    val dependencyNode = dependencyNodes[j]
                    val dependsOn = dependencyNode.providableVersionNodes.singleOrNull { it in availablePluginNodeSet }

                    // 若被依赖的插件存在，或并非硬性依赖，则视为此依赖可以被满足。
                    // 此时不需要检查 original，因为在前面构造依赖图的时候已经筛去相关边。
                    val dependencySatisfied = dependsOn != null || !dependencyNode.dependency.required

                    if (dependencySatisfied) {
                        if (dependsOn != null) {
                            val dependsOnPlugin = dependsOn.pluginEntry.plugin
                            val dependsOnIndex = availablePluginNodesToIndex[dependsOn.pluginEntry]
                                ?: error("Plugin ${dependsOnPlugin.id}:${dependsOnPlugin.version} not found in available plugin nodes.")

                            // 连接依赖关系，在此过程中会检查循环依赖。
                            val dependsOnDependencyGraphNode = traversingDependencyGraphNodes[dependsOnIndex]
                            traversingDependencyGraph.link(traversingDependencyGraphNode, dependsOnDependencyGraphNode)
                        }
                    } else {
                        logger.trace {
                            val plugin = pair.second.pluginEntry.plugin
                            "The ${j + 1}-th dependency of plugin ${plugin.meta.id}:${plugin.version} is not satisfied: ${dependencyNode.dependency}"
                        }
                    }
                }
            }

            // 此处需要考虑被间接依赖的插件能否满足。例如，自己的依赖插件满足，但是它们却无法启动，那最终还是无法启动。
            traversingDependencyGraph.traverse(platform) {
                // 检查依赖关系是否满足。

            }

            solutionIndex++
        }

        println(dependencyGraph)
    }
}