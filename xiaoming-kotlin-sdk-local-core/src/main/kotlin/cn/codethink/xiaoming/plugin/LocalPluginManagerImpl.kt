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
import cn.codethink.xiaoming.util.DirectedAcyclicGraph
import cn.codethink.xiaoming.util.DualKeyMap
import cn.codethink.xiaoming.util.ExperimentalApi
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MutableDirectedAcyclicGraphImpl
import cn.codethink.xiaoming.util.MutableDualKeyMap
import cn.codethink.xiaoming.util.MutableDualKeyMapImpl
import cn.codethink.xiaoming.util.MutableMapRegistrationManagerImpl
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.Registration
import cn.codethink.xiaoming.util.SdkConstants
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.getOrFail
import cn.codethink.xiaoming.util.productSize
import cn.codethink.xiaoming.util.products
import cn.codethink.xiaoming.util.runIf
import cn.codethink.xiaoming.util.runIfNotNull
import cn.codethink.xiaoming.util.toMutableDualKeyMap
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.jvm.jvmName

@OptIn(InternalApi::class, ExperimentalApi::class)
class LocalPluginManagerImpl(
    override val platform: LocalPlatform
) : LocalPluginManager {
    private val logger: KLogger = KotlinLogging.logger(PluginManager::class.jvmName)

    // 具有相同 ID，但用不同版本的插件可以并存，但是只有一个可以加载。
    private var mutableAvailablePlugins = MutableDualKeyMapImpl<NamespaceId, Version, AbstractLocalServingPlugin>()
    override val availablePlugins: Collection<Plugin> get() = mutableAvailablePlugins.values.toList()

    // 用于保护下面 plugins 和 providerPlugins 表的锁。
    private val lock = ReentrantReadWriteLock()

    // 插件一旦加载，便需先将自己置于其中。以免相同 ID，不同版本的插件被同时加载。
    private var mutablePlugins = mutableMapOf<NamespaceId, AbstractLocalServingPlugin>()
    override val plugins: Map<NamespaceId, Plugin> get() = lock.read { mutablePlugins.toMap() }

    // 插件的提供者插件。
    private var mutableProviderPlugins = mutableMapOf<NamespaceId, AbstractLocalServingPlugin>()
    override val providerPlugins: Map<NamespaceId, Plugin> get() = lock.read { mutableProviderPlugins.toMap() }

    private val mutablePluginScanners = MutableMapRegistrationManagerImpl<String, PluginScanner>()
    override val pluginScanners: Map<String, Registration<PluginScanner>> get() = mutablePluginScanners.toRegistrationMap()

    private val mutablePluginSources = MutableMapRegistrationManagerImpl<String, PluginSource>()
    override val pluginSources: Map<String, Registration<PluginSource>> get() = mutablePluginSources.toRegistrationMap()

    private sealed interface AvailablePluginEntry {
        val plugin: AbstractLocalServingPlugin
    }

    // 原本就存在于系统中的插件 Entry
    private class OriginalPluginEntry(
        override val plugin: AbstractLocalServingPlugin
    ) : AvailablePluginEntry

    // 将会被安装的插件 Entry
    private class InstallingPluginEntry(
        override val plugin: AbstractLocalServingPlugin,
        val pluginSourceKey: String
    ) : AvailablePluginEntry

    // 必须处理的插件 Entry。
    private class RequiredPluginEntry(
        override val plugin: AbstractLocalServingPlugin
    ) : AvailablePluginEntry

    private class PluginDependencyGraph {
        class PluginNode(val id: NamespaceId) {
            val versionNodes: MutableMap<Version, PluginVersionNode> = mutableMapOf()

            fun getOrCreateVersionNode(version: Version): PluginVersionNode {
                return versionNodes.computeIfAbsent(version) { PluginVersionNode(this, it) }
            }

            override fun toString(): String {
                return "PluginNode($id)"
            }
        }

        class PluginVersionNode(
            val pluginNode: PluginNode,
            val version: Version
        ) {
            // 谁可以提供这个版本？包括这个插件本身。
            val providerNodes: MutableList<AvailablePluginNode> = mutableListOf()

            override fun toString(): String {
                return "PluginVersionNode(${pluginNode.id}:$version)"
            }
        }

        class AvailablePluginNode(
            val pluginVersionNode: PluginVersionNode,
            val pluginEntry: AvailablePluginEntry
        ) {
            // 这个插件依赖于哪些其他的插件？
            val dependencyNodes: MutableList<AvailablePluginDependencyNode> = mutableListOf()

            // 这个插件的依赖关系有可能被满足吗（true 不一定就能满足，但 false 一定不能满足）
            var mayDependencySatisfied = true

            // 这个插件提供了哪些其他插件？(provisionIndex?, provision)
            val providableVersionNodes: MutableList<Pair<Int?, PluginVersionNode>> = mutableListOf()

            override fun toString(): String {
                return "AvailablePluginNode(${pluginEntry.plugin.meta.id}:${pluginEntry.plugin.version})"
            }
        }

        class AvailablePluginDependencyNode(
            val pluginNode: PluginNode,
            val dependency: PluginDependency
        ) {
            // 这个依赖可以被哪些插件满足？
            val providableNodes: MutableList<AvailablePluginNode> = mutableListOf()

            override fun toString(): String {
                return "AvailablePluginDependencyNode(${pluginNode.id}:${dependency.id})"
            }
        }

        val pluginNodes: MutableMap<NamespaceId, PluginNode> = mutableMapOf()

        val availablePluginNodesByKey: MutableDualKeyMap<NamespaceId, Version, AvailablePluginNode> = MutableDualKeyMapImpl()
        val availablePluginNodesByPlugin: MutableMap<AbstractPlugin, AvailablePluginNode> = mutableMapOf()

        // 可以提供某个插件某个版本的所有插件（包含它自己）的节点：(自己的第几条 provision, 自己)
        val providablePluginVersionNodes: MutableDualKeyMap<NamespaceId, Version, MutableList<Pair<Int?, AvailablePluginNode>>> = MutableDualKeyMapImpl()

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
                for ((provisionIndex, provision) in availablePluginNode.pluginEntry.plugin.meta.provisions.withIndex()) {
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
                        availablePluginNode.providableVersionNodes.add(provisionIndex to provisionVersionNode)
                    }

                }

                // 连接插件和它自己的提供关系。
                versionNode.providerNodes.add(availablePluginNode)
                availablePluginNode.providableVersionNodes.add(null to versionNode)

                // 添加提供列表。
                for ((provisionIndex, providableVersion) in availablePluginNode.providableVersionNodes) {
                    providablePluginVersionNodes.computeIfAbsent(
                        providableVersion.pluginNode.id, providableVersion.version
                    ) { mutableListOf() }.add(provisionIndex to availablePluginNode)
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
                        .runIf(dependency.original) { filter { it.second.pluginEntry.plugin.id == dependency.id } }

                    for (value in dependencyPluginVersionNodes) {
                        dependencyNode.providableNodes.add(value.second)
                    }
                }
            }
        }
    }

    private class PluginDependencySolutionContext(
        val entry: AvailablePluginEntry,
        val provisions: List<PluginSignature?>,

        // 与插件元数据的 dependencies 一一对应。
        val dependencies: List<PluginDependencyGraph.AvailablePluginNode?>
    ) {
        // 依赖关系是否有可能满足。判断依据是 dependencies 内的 null。
        var mayDependencySatisfied = true

        // 准确结果，判断依据是所有直接或间接依赖的插件的 mayDependencySatisfied。
        var dependencySatisfied: Boolean? = null

        override fun toString(): String {
            return "PluginDependencySolutionContext(entry=$entry, provisions=$provisions, dependencies=$dependencies)"
        }
    }

    // 在激活（加载或启动）插件时可能需要按需下载依赖插件，并先激活其依赖插件再激活自己。
    private abstract inner class ActivatePluginDependencyResolver(val operation: Operation, val block: suspend (AbstractLocalServingPlugin) -> Unit) {
        abstract val availablePluginEntries: MutableDualKeyMap<NamespaceId, Version, AvailablePluginEntry>

        suspend fun resolveAndActivateDependencies() {
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

                                val newPlugin = createPlugin(meta, entry.configuration, handler, operation)
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

                                        is RequiredPluginEntry -> {
                                            logger.trace {
                                                "Plugin ${now.plugin.meta.id}:${now.plugin.version} is required to be handled, " +
                                                        "so another one to be installed will be ignored."
                                            }
                                            now
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
                    if (dependencyNode.providableNodes.isEmpty() && dependencyNode.dependency.required) {
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
            val pluginAvailableNodeCandidates = mutableMapOf<NamespaceId, List<Pair<Version, Pair<Int?, PluginDependencyGraph.AvailablePluginNode>>>>()
            for ((pluginId, versionProviders) in providablePluginVersionNodes.firstEntries) {
                val candidatesBeforeFiltering = versionProviders
                    .flatMap { it.value.map { e -> it.key to e } }

                val candidates = candidatesBeforeFiltering
                    .filter { it.second.second.mayDependencySatisfied }
                    .sortedWith { left, right ->
                        val compareVersion = right.first.compareTo(left.first)
                        if (compareVersion != 0) {
                            return@sortedWith compareVersion
                        }

                        val leftProvisionCount = left.second.second.pluginEntry.plugin.meta.provisions.size
                        val rightProvisionCount = right.second.second.pluginEntry.plugin.meta.provisions.size

                        return@sortedWith rightProvisionCount - leftProvisionCount
                    }

                if (candidates.isEmpty()) {
                    logger.trace {
                        val beforeFiltering = candidatesBeforeFiltering.joinToString {
                            val plugin = it.second.second.pluginEntry.plugin
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
            logger.trace { "Found $solutionCount solutions for plugin dependency resolution." }
            solutionLoop@ for ((solutionIndex, availablePluginNodes) in availablePluginNodeCandidates.products().withIndex()) {
                logger.trace {
                    val solution = availablePluginNodes.joinToString {
                        val plugin = it.second.second.pluginEntry.plugin
                        "${plugin.id}:${plugin.version}"
                    }
                    "Try to resolve plugin dependency with the $solutionIndex / $solutionCount solution: $solution"
                }

                // 检查当前方案是否存在同一个插件的多个版本，若有直接舍弃。
                val availablePluginIdToNodes = mutableMapOf<NamespaceId, PluginDependencyGraph.AvailablePluginNode>()
                for (availablePluginNode in availablePluginNodes) {
                    val newNode = availablePluginNode.second.second
                    val oldNode = availablePluginIdToNodes.computeIfAbsent(availablePluginNode.second.second.pluginEntry.plugin.id) { newNode }
                    if (oldNode != newNode) {
                        logger.trace {
                            "Plugin ${availablePluginNode.second.second.pluginEntry.plugin.id} has multiple versions: " +
                                    "${oldNode.pluginEntry.plugin.version} vs ${newNode.pluginEntry.plugin.version}, " +
                                    "The solution is invalid."
                        }
                        continue@solutionLoop
                    }
                }

                val availablePluginNodeCount = availablePluginNodes.size

                // 计算插件都是被谁提供的。注意此处可能包含提供冲突。

                // 插件 ID -> ( 插件版本, ( 提供者 provision index, 提供者 ) )
                // 每个插件要想启动，必须提供自己的功能。
                val providerNodes = availablePluginIdToNodes.mapValues {
                    it.value.pluginEntry.plugin.version to ((null as Int?) to it.value)
                }.toMutableMap()

                val provisionSignatures = providerNodes.mapValues { mutableListOf(it.value.second.second.pluginEntry.plugin.signature) }
                val provisionConflictedPluginIds = mutableSetOf<NamespaceId>()

                // 当前方案所对应的每种（具备能力的）插件的版本。
                val solutionProvidedPluginVersions = mutableMapOf<NamespaceId, Version>()
                for (i in 0 until availablePluginNodeCount) {
                    val pair = availablePluginNodes[i]

                    val provisionId = pluginAvailableNodeCandidateList[i].key
                    val provisionVersion = pair.first

                    solutionProvidedPluginVersions[provisionId] = provisionVersion
                }

                // 检查是否存在提供冲突。
                availablePluginLoop@ for (availablePluginNode in availablePluginIdToNodes.values) {
                    // 尝试为其他人提供功能。
                    val plugin = availablePluginNode.pluginEntry.plugin
                    for ((index, provision) in plugin.meta.provisions.withIndex()) {
                        // 检查“我提供的插件”此前是否已经有人提供过了。
                        val oldProviderNode = providerNodes[provision.id]

                        // 看看当前方案是否需要提供这个插件。
                        val expectedVersion = solutionProvidedPluginVersions[provision.id]

                        // 如果当前方案需要这一插件，则看看是否有能力提供。如果可以，那很好了。
                        // 看下这个插件此前是否有提供方了。没人提供，正合我意。
                        if (expectedVersion != null && provision.matches(provision.id, expectedVersion) && oldProviderNode == null) {
                            providerNodes[provision.id] = expectedVersion to (index to availablePluginNode)
                            provisionSignatures.getOrFail(plugin.id).add(PluginSignature(provision.id, expectedVersion))
                            continue
                        }

                        // 如果可以不提供，那很好了，装作无事发生即可。
                        if (provision.optional) {
                            continue
                        }

                        // 如果不能不提供，则只能放弃启动这个插件。
                        provisionConflictedPluginIds.add(plugin.id)
                        continue@availablePluginLoop
                    }
                }

                // solution 开头的容器内去除了具有 provide 冲突的插件，但是还没去掉依赖无法满足的插件。
                val solutionProviderNodes = providerNodes.runIf(provisionConflictedPluginIds.isNotEmpty()) {
                    filterValues { it.second.second.pluginEntry.plugin.id !in provisionConflictedPluginIds }
                }
                val solutionProvisionSignatures = provisionSignatures.runIf(provisionConflictedPluginIds.isNotEmpty()) {
                    filterKeys { it !in provisionConflictedPluginIds }
                }
                val solutionPluginNodes = solutionProviderNodes.values.map { it.second.second }.associateBy { it.pluginEntry.plugin.id }

                if (solutionProviderNodes.isEmpty() || solutionProvisionSignatures.isEmpty()) {
                    logger.trace { "No solution found for plugin dependency resolution." }
                    continue
                }

                // 计算依赖关系，构建依赖关系遍历图，去除无法满足依赖关系的节点。
                val graph = MutableDirectedAcyclicGraphImpl<PluginDependencySolutionContext, Unit>()
                val graphNodes = solutionPluginNodes.map {
                    val context = PluginDependencySolutionContext(
                        entry = it.value.pluginEntry,
                        provisions = solutionProvisionSignatures.getOrFail(it.key),
                        dependencies = it.value.pluginEntry.plugin.meta.dependencies.map { dep -> solutionProviderNodes[dep.id]?.second?.second }
                    )
                    graph.allocate(context)
                }
                val entryToGraphNodes = graphNodes.associateBy { it.value.entry }

                // 尝试构建依赖关系图并为节点染色
                for (graphNode in graphNodes) {
                    for ((dependency, dependencyNode) in graphNode.value.entry.plugin.meta.dependencies.zip(graphNode.value.dependencies)) {
                        if (dependencyNode == null && dependency.required) {
                            logger.trace {
                                "Plugin ${graphNode.value.entry.plugin.meta.id}:${graphNode.value.entry.plugin.version} " +
                                        "has a dependency ${dependency.id}:${dependency.version} which is not satisfied."
                            }

                            graphNode.value.mayDependencySatisfied = false
                            break
                        }
                    }
                }

                // 连接依赖关系，检查循环依赖，并准备做递归依赖检查。
                for (graphNode in graphNodes) {
                    if (graphNode.value.mayDependencySatisfied) {
                        for (dependency in graphNode.value.dependencies) {
                            if (dependency == null) {
                                continue
                            }

                            val dependencyNode = entryToGraphNodes[dependency.pluginEntry] ?: continue
                            graph.link(dependencyNode, graphNode, Unit)
                        }
                    }
                }

                // 做最后一次依赖检查。
                fun DirectedAcyclicGraph.Node<PluginDependencySolutionContext, Unit>.isDependencySatisfied(): Boolean {
                    if (!value.mayDependencySatisfied) {
                        return false
                    }

                    var result = value.dependencySatisfied
                    if (result == null) {
                        val allPredecessors = allPredecessors
                        if (allPredecessors.isEmpty()) {
                            return true
                        }

                        result = allPredecessors.all { it.isDependencySatisfied() }
                    }
                    return result
                }

                val finalGraphNodeContexts = graphNodes.filter { it.isDependencySatisfied() }.map { it.value }
                val finalGraph = if (finalGraphNodeContexts.size == graphNodes.size) graph else {
                    val result = MutableDirectedAcyclicGraphImpl<PluginDependencySolutionContext, Unit>()

                    val finalGraphNodes = finalGraphNodeContexts.map { result.allocate(it) }
                    val entryToFinalGraphNodes = finalGraphNodes.associateBy { it.value.entry }

                    for (graphNode in finalGraphNodes) {
                        for (dependency in graphNode.value.dependencies) {
                            if (dependency == null) {
                                continue
                            }

                            val dependencyNode = entryToFinalGraphNodes[dependency.pluginEntry] ?: continue
                            result.link(dependencyNode, graphNode, Unit)
                        }
                    }

                    result
                }

                if (tryActivatePlugins(finalGraph)) {
                    return
                }
            }

            error("No solution found for plugin dependency resolution.")
        }

        open suspend fun tryActivatePlugins(solution: DirectedAcyclicGraph<PluginDependencySolutionContext, Unit>): Boolean {
            var result = true
            solution.forEachConcurrently {
                if (!result) {
                    return@forEachConcurrently
                }

                val plugin = it.value.entry.plugin
                val requiredDependencies = it.value.dependencies.map { dep -> dep?.pluginEntry?.plugin }
                if (plugin.isLoadingOrLoaded) {
                    if (plugin.dependencies != requiredDependencies || plugin.provisions != it.value.provisions) {
                        logger.trace {
                            "Plugin ${plugin.meta.id}:${plugin.version} is already loaded, " +
                                    "but its dependencies or provisions are changed. " +
                                    "Please unload it first before loading again."
                        }
                        result = false
                        return@forEachConcurrently
                    }
                } else {
                    plugin.dependencies = requiredDependencies
                    plugin.provisions = it.value.provisions
                }

                // 直接让把异常往调用者那边抛。
                block(it.value.entry.plugin)

//                try {
//                    block(it.value.entry.plugin)
//                } catch (t: Throwable) {
//                    logger.warn(t) { "Exception thrown while activating ${plugin.signature}. " }
//                    result = false
//                    return@forEachConcurrently
//                }
            }

            return result
        }
    }

    private fun DualKeyMap<NamespaceId, Version, AbstractLocalServingPlugin>.mapToOriginalPluginEntries(): MutableDualKeyMap<NamespaceId, Version, AvailablePluginEntry> {
        return mapValues { OriginalPluginEntry(it.value) }.toMutableDualKeyMap()
    }

    private abstract inner class AbstractLocalServingPlugin(
        meta: PluginMeta, val configuration: PluginConfiguration, handler: PluginHandler, operation: Operation
    ) : AbstractPlugin(
        meta, handler, this, operation, PluginState.UNALLOCATED, platform
    ) {
        override var dependencies: List<Plugin?> = emptyList()
        override var provisions: List<PluginSignature?> = emptyList()

        private inner class ActivateThisPluginDependencyResolver(
            operation: Operation, block: suspend (AbstractLocalServingPlugin) -> Unit
        ) : ActivatePluginDependencyResolver(operation, block) {
            override val availablePluginEntries: MutableDualKeyMap<NamespaceId, Version, AvailablePluginEntry> = mutableAvailablePlugins
                .mapToOriginalPluginEntries()
                .apply {
                    this[id, version] = RequiredPluginEntry(this@AbstractLocalServingPlugin)
                }

            override suspend fun tryActivatePlugins(solution: DirectedAcyclicGraph<PluginDependencySolutionContext, Unit>): Boolean {
                val containsRequiredPluginEntry = solution.nodes.any { it.value.entry is RequiredPluginEntry }
                if (!containsRequiredPluginEntry) {
                    return false
                }

                return super.tryActivatePlugins(solution)
            }
        }

        override suspend fun load(operation: Operation) {
            ActivateThisPluginDependencyResolver(operation) { it.ensureDoLoad(operation) }.resolveAndActivateDependencies()
        }

        override suspend fun onDoLoad(operation: Operation) {
            acquireUniquePluginLock(operation)
        }

        override suspend fun enable(operation: Operation) {
            ActivateThisPluginDependencyResolver(operation) { it.ensureDoEnable(operation) }.resolveAndActivateDependencies()
        }

        override suspend fun disable(operation: Operation) {
            allDependencies.forEach { it.ensureDisabled(operation) }
            doDisable(operation)
        }

        override suspend fun unload(operation: Operation) {
            allDependencies.forEach { it.ensureUnloaded(operation) }
            doUnload(operation)
        }

        override suspend fun release(operation: Operation) {
            ensureUnloaded(operation)
            doRelease(operation)
        }

        override suspend fun onDoUnloaded(operation: Operation) {
            onDoUnloadedOrCrashed()
        }

        override suspend fun onDoCrashed(operation: Operation) {
            onDoUnloadedOrCrashed()
            onDoReleasedOrCrashed()
        }

        private fun onDoUnloadedOrCrashed() {
            releaseUniquePluginLock()

            provisions = emptyList()
            dependencies = emptyList()
        }

        override suspend fun onDoReleased(operation: Operation) {
            onDoReleasedOrCrashed()
        }

        private fun onDoReleasedOrCrashed() {
            mutableAvailablePlugins.remove(id, version)
        }

        override suspend fun crash(operation: Operation) {
            if (isLoadingOrLoaded) {
                TODO()
            }
            doCrash(operation)
        }

        // 尝试为本插件获取 LOAD 锁。返回持有独占当前 ID 启动权的插件。若为 this 表示获取成功。
        private fun tryAcquireUniquePluginLock(): AbstractLocalServingPlugin {
            return lock.write {
                val oldPlugin = mutablePlugins[id]
                val oldProvider = mutableProviderPlugins[id]

                if (oldPlugin != null) {
                    return oldPlugin
                }
                if (oldProvider != null) {
                    return oldProvider
                }

                mutablePlugins[id] = this
                mutableProviderPlugins[id] = this
                for (provision in provisions) {
                    val provisionId = provision?.id ?: continue
                    mutableProviderPlugins[provisionId] = this
                }

                this
            }
        }

        private suspend fun acquireUniquePluginLock(operation: Operation) {
            var oldPlugin = tryAcquireUniquePluginLock()
            while (oldPlugin !== this) {
                if (oldPlugin.configuration.retainOnConflict) {
                    error(
                        "There is another plugin ${oldPlugin.meta.id} with version ${oldPlugin.version} is already loaded, " +
                                "and its `retainOnConflict` is set to true. " +
                                "Note that multiple plugins with the same ID are not allowed to be loaded at the same time. " +
                                "Please unload the old one explicitly first."
                    )
                }

                // 抢夺插件锁，需要先把对方插件卸载。
                oldPlugin.ensureUnloaded(operation)
                oldPlugin = tryAcquireUniquePluginLock()
            }
        }

        // 插件被成功 UNLOAD，释放独占当前 ID 的锁。若返回 null 表示成功（也可能从未获取），否则失败。
        private fun tryReleaseUniquePluginLock(): Plugin? {
            return lock.write {
                val oldPlugin = mutablePlugins[id]
                if (oldPlugin == this) {
                    mutablePlugins.remove(id)
                } else {
                    return oldPlugin
                }

                mutableProviderPlugins.remove(id)
                for (provision in provisions) {
                    val provisionId = provision?.id ?: continue
                    mutableProviderPlugins.remove(provisionId)
                }

                null
            }
        }

        private fun releaseUniquePluginLock() {
            val nowPlugin = tryReleaseUniquePluginLock()
            check(nowPlugin == null) {
                "Plugin ${meta.id} is not loaded and its version is ${nowPlugin?.version}. " +
                        "Fail to release unique plugin lock for another version ${version}."
            }
        }
    }

    private inner class SharablePluginImpl(
        meta: PluginMeta, configuration: PluginConfiguration, handler: PluginHandler, operation: Operation
    ) : AbstractLocalServingPlugin(
        meta, configuration, handler, operation
    ), SharablePlugin {
        private val mutableInstances: MutableMap<RemotePlatform, RemoteServingPlugin> = ConcurrentHashMap()
        override val instances: Map<RemotePlatform, RemoteServingPlugin> get() = mutableInstances.toMap()

        private inner class RemoteServingPluginImpl(
            meta: PluginMeta, operation: Operation, handler: PluginHandler, platform: RemotePlatform,
            override var provisions: List<PluginSignature?>,
            override var dependencies: List<Plugin?>
        ) : AbstractPlugin(
            meta, handler, this@LocalPluginManagerImpl, operation, PluginState.ALLOCATED, platform
        ), RemoteServingPlugin {
            override val backend: SharablePlugin get() = this@SharablePluginImpl

            @ExperimentalApi
            override suspend fun load(operation: Operation) {
                doLoad(operation)
            }

            @ExperimentalApi
            override suspend fun enable(operation: Operation) {
                doEnable(operation)
            }

            @ExperimentalApi
            override suspend fun disable(operation: Operation) {
                doDisable(operation)
            }

            @ExperimentalApi
            override suspend fun unload(operation: Operation) {
                doUnload(operation)
            }

            @ExperimentalApi
            override suspend fun release(operation: Operation) {
                doRelease(operation)
            }

            override suspend fun crash(operation: Operation) {
                doCrash(operation)
            }

            override suspend fun onDoUnloaded(operation: Operation) {
                onUnloadedOrCrashed()
            }

            override suspend fun onDoCrashed(operation: Operation) {
                onUnloadedOrCrashed()
            }

            private fun onUnloadedOrCrashed() {
                dependencies = emptyList()
                provisions = emptyList()
            }

            override suspend fun onDoReleased(operation: Operation) {
                mutableInstances.remove(platform)
            }
        }

        fun registerInstance(
            platform: RemotePlatform,
            handler: PluginHandler,
            provisions: List<PluginSignature?>,
            dependencies: List<Plugin?>,
            operation: Operation
        ): RemoteServingPlugin {
            val newPlugin = RemoteServingPluginImpl(meta, operation, handler, platform, provisions, dependencies)
            val oldPlugin = mutableInstances.computeIfAbsent(platform) { newPlugin }
            require(oldPlugin === newPlugin) { "Plugin ${meta.id} is already registered" }
            return newPlugin
        }
    }

    private inner class PluginImpl(
        meta: PluginMeta, configuration: PluginConfiguration, handler: PluginHandler, operation: Operation
    ) : AbstractLocalServingPlugin(meta, configuration, handler, operation)

    private fun createPlugin(meta: PluginMeta, configuration: PluginConfiguration, handler: PluginHandler, operation: Operation): AbstractLocalServingPlugin {
        return when (configuration.sharable) {
            true -> SharablePluginImpl(meta, configuration, handler, operation)
            false -> PluginImpl(meta, configuration, handler, operation)
        }
    }

    override fun registerPlugin(meta: PluginMeta, configuration: PluginConfiguration, handler: PluginHandler, operation: Operation): Plugin {
        val newPlugin = createPlugin(meta, configuration, handler, operation)
        val oldPlugin = mutableAvailablePlugins.putIfAbsent(meta.id, meta.version, newPlugin)
        require(oldPlugin == null) { "Plugin ${meta.id} is already registered" }
        return newPlugin
    }

    override fun getPlugin(id: NamespaceId): Plugin? {
        return lock.read { mutablePlugins[id] }
    }

    override fun getAvailablePlugin(id: NamespaceId, version: Version): Plugin? {
        return mutableAvailablePlugins[id, version]
    }

    override fun getAvailablePlugin(signature: PluginSignature): Plugin? {
        return getAvailablePlugin(signature.id, signature.version)
    }

    override fun getAvailablePlugins(id: NamespaceId): Map<Version, Plugin> {
        return mutableAvailablePlugins[id]
    }

    override fun getAvailablePlugins(pattern: PluginPattern): Map<Version, Plugin> {
        return getAvailablePlugins(pattern.id).runIfNotNull(pattern.version) { p -> filterKeys { p.matches(it) } }
    }

    override fun getProviderPlugin(id: NamespaceId): Plugin? {
        return lock.read { mutableProviderPlugins[id] }
    }

    private inner class PluginScanContextImpl(override val operation: Operation) : PluginScanContext {
        val newAvailablePlugins: MutableDualKeyMapImpl<NamespaceId, Version, AbstractLocalServingPlugin> = MutableDualKeyMapImpl()

        override fun registerPlugin(meta: PluginMeta, configuration: PluginConfiguration, operation: Operation, handler: PluginHandler) {
            val newPlugin = createPlugin(meta, configuration, handler, operation)
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

    private inner class ActivateAvailablePluginsDependencyResolver(
        operation: Operation, block: suspend (AbstractLocalServingPlugin) -> Unit
    ) : ActivatePluginDependencyResolver(operation, block) {
        override val availablePluginEntries: MutableDualKeyMap<NamespaceId, Version, AvailablePluginEntry> =
            mutableAvailablePlugins.mapToOriginalPluginEntries()
    }

    override suspend fun loadPlugins(operation: Operation) {
        ActivateAvailablePluginsDependencyResolver(operation) { it.ensureDoLoad(operation) }.resolveAndActivateDependencies()
    }

    override suspend fun enablePlugins(operation: Operation) {
        ActivateAvailablePluginsDependencyResolver(operation) { it.ensureDoEnable(operation) }.resolveAndActivateDependencies()
    }

    override suspend fun disablePlugins(operation: Operation) {
        ActivateAvailablePluginsDependencyResolver(operation) { it.ensureDoDisable(operation) }.resolveAndActivateDependencies()
    }

    override suspend fun unloadPlugins(operation: Operation) {
        for (availablePlugin in availablePlugins) {
            availablePlugin.ensureUnloaded(operation)
        }
    }

    override suspend fun releasePlugins(operation: Operation) {
        for (availablePlugin in availablePlugins) {
            availablePlugin.ensureReleased(operation)
        }
    }
}