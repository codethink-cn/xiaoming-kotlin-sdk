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
import cn.codethink.xiaoming.util.MutableRegistration
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.Registration
import cn.codethink.xiaoming.util.Version
import io.github.oshai.kotlinlogging.KLogger
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

/**
 * 本地插件管理器。
 *
 * @author Chuanwise
 */
interface LocalPluginManager : PluginManager {
    /**
     * 宿主。
     */
    override val platform: LocalPlatform

    /**
     * 插件管理器日志。
     */
    val logger: KLogger

    /**
     * 已经启动的插件。
     */
    override val plugins: Map<NamespaceId, AvailablePlugin>

    /**
     * 宿主的所有插件，其中可能包括已识别，但未加载的插件。
     */
    val installedPlugins: Collection<AvailablePlugin>

    /**
     * 插件扫描器，用于在必要时扫描一次宿主上的所有插件。
     */
    val pluginScanners: Map<String, Registration<PluginScanner>>

    /**
     * 插件源，用于在必要时安装插件。
     */
    val pluginSources: Map<String, Registration<PluginSource>>

    override fun getPlugin(id: NamespaceId): AvailablePlugin?

    override fun getPluginOrFail(id: NamespaceId): AvailablePlugin

    override fun getProviderPlugin(id: NamespaceId): AvailablePlugin?

    override fun getProviderPluginOrFail(id: NamespaceId): Plugin

    /**
     * 通过 ID 和版本获取插件。
     *
     * @param id 插件 ID
     * @param version 插件版本
     * @return 插件
     */
    fun getInstalledPlugin(id: NamespaceId, version: Version): AvailablePlugin?

    fun getInstalledPluginOrFail(id: NamespaceId, version: Version): AvailablePlugin

    /**
     * 通过插件签名获取插件。
     *
     * @param signature 插件签名
     * @return 插件
     */
    fun getInstalledPlugins(signature: PluginSignature): AvailablePlugin?

    /**
     * 获取已经分配的一个插件的所有版本。
     *
     * @param id 插件 ID
     * @return 插件版本
     */
    fun getInstalledPlugins(id: NamespaceId): Map<Version, AvailablePlugin>

    /**
     * 获取已经分配的一个插件的所有版本。
     *
     * @param pattern 插件模式
     * @return 插件版本
     */
    fun getInstalledPlugins(pattern: PluginPattern): Map<Version, AvailablePlugin>

    /**
     * 注册一个插件。
     *
     * @param meta 插件元数据
     * @param configuration 插件配置
     * @param operation 操作原因
     * @param handler 插件处理器
     * @return 插件
     */
    fun registerInstalledPlugin(meta: PluginMeta, configuration: PluginConfiguration, handler: PluginHandler, operation: Operation): AvailablePlugin

    /**
     * 解析一个插件，其将执行一次扫描和插件源请求。
     *
     * @param pattern 插件模式
     * @param operation 解析插件的原因
     * @return 插件
     */
    @JvmBlockingBridge
    suspend fun resolvePlugins(pattern: PluginPattern, operation: Operation): Map<Version, AvailablePlugin>

    /**
     * 刷新插件列表。
     *
     * @param operation 操作原因
     */
    @JvmBlockingBridge
    suspend fun flushAvailablePlugins(operation: Operation)

    @JvmBlockingBridge
    suspend fun loadPlugins(operation: Operation)

    @JvmBlockingBridge
    suspend fun enablePlugins(operation: Operation)

    @JvmBlockingBridge
    suspend fun disablePlugins(operation: Operation)

    @JvmBlockingBridge
    suspend fun unloadPlugins(operation: Operation)

    @JvmBlockingBridge
    suspend fun releasePlugins(operation: Operation)

    fun registerPluginScanner(id: String, scanner: PluginScanner, operation: Operation): MutableRegistration<PluginScanner>

    fun registerPluginSource(id: String, source: PluginSource, operation: Operation): MutableRegistration<PluginSource>
}