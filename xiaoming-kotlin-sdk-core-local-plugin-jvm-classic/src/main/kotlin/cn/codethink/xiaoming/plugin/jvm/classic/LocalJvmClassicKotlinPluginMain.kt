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

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.plugin.id
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class LocalJvmKotlinClassicPluginContext(
    override val plugin: LocalJvmClassicPlugin,
    override val cause: Cause,
    parentJob: Job,
    parentCoroutineContext: CoroutineContext,
    override val error: Cause? = null
) : LocalJvmClassicPluginContext, CoroutineScope {
    private val supervisorJob = SupervisorJob(parentJob)
    private val scope = CoroutineScope(supervisorJob + parentCoroutineContext)
    override val coroutineContext: CoroutineContext by scope::coroutineContext

    val logger = KotlinLogging.logger(plugin.meta.logger ?: plugin.id.toString())
}

/**
 * The main class of a local JVM classic kotlin plugin.
 *
 * @author Chuanwise
 * @see LocalJvmKotlinClassicPluginContext
 */
@LocalJvmClassicPluginMain(entry = LocalJvmClassicKotlinPluginMainEntryFactory::class)
interface LocalJvmClassicKotlinPluginMain {
    /**
     * Do some initial works when the plugin is loading.
     *
     * Loading maybe randomly and concurrent, do not rely on its sort. Make sure just
     * do operations like resources initialization and required environments checking.
     *
     * If an unexpected error occurred, throw an exception to prevent the plugin from loading,
     * or set [LocalJvmKotlinClassicPluginContext.error] to a non-null value to mark the
     * plugin as errored.
     *
     * If it's null and no exception thrown, the plugin is considered as loaded successfully.
     *
     * Don't use [context] to launch coroutines, because the plugin is not enabled yet.
     * Only enabled plugins can launch coroutines.
     *
     * @param context the context of this plugin.
     */
    fun onLoad(context: LocalJvmKotlinClassicPluginContext) = Unit

    /**
     * Activate the plugin, and do some works to make the plugin available.
     *
     * When platform call this function, it's guaranteed that:
     *
     * 1. [onLoad] called successfully.
     * 2. all required dependencies are enabled successfully.
     * 3. platform already tried its best to enable the most of the optional dependencies.
     * 4. None of plugins depending on this plugin are enabled.
     *
     * If an unexpected error occurred, throw an exception to prevent the plugin from enabling,
     * or set [LocalJvmKotlinClassicPluginContext.error] to a non-null value to mark the
     * plugin as errored.
     *
     * If it's null and no exception thrown, the plugin is considered as enabled successfully.
     *
     * To launch coroutines, please use [context], they will be automatically cancelled on
     * [onDisable].
     *
     * @param context the context of this plugin.
     */
    fun onEnable(context: LocalJvmKotlinClassicPluginContext) = Unit

    /**
     * Deactivate the plugin, and do some works to make the plugin unavailable.
     *
     * When platform call this function, it's guaranteed that:
     *
     * 1. [onEnable] called successfully.
     * 2. All plugins depending on this plugin are disabled.
     *
     * If an unexpected error occurred, throw an exception to prevent the plugin from disabling,
     * or set [LocalJvmKotlinClassicPluginContext.error] to a non-null value to mark the
     * plugin as errored.
     *
     * If it's null and no exception thrown, the plugin is considered as disabled successfully.
     *
     * DON't remember to clean up resources (especially related to dependencies).
     *
     * @param context the context of this plugin.
     */
    fun onDisable(context: LocalJvmKotlinClassicPluginContext) = Unit
}

/**
 * Notice that the implementation is not thread safe. [LocalJvmClassicPlugin] will maintain a single
 * instance of this invoker, and make sure that it's only called by one thread at a time.
 *
 * @author Chuanwise
 */
class LocalJvmClassicKotlinPluginMainEntry(
    val main: LocalJvmClassicKotlinPluginMain
) : LocalJvmClassicPluginMainEntry {
    private var context: LocalJvmKotlinClassicPluginContext? = null

    override fun onLoad(plugin: LocalJvmClassicPlugin, cause: Cause) {
        var context = context
        if (context == null) {
            context = LocalJvmKotlinClassicPluginContext(
                plugin, cause, plugin.platformApi.supervisorJob, plugin.platformApi.coroutineContext, null
            )
        }

        main.onLoad(context)
    }

    override fun onEnable(plugin: LocalJvmClassicPlugin, cause: Cause) {
        TODO("Not yet implemented")
    }

    override fun onDisable(plugin: LocalJvmClassicPlugin, cause: Cause) {
        TODO("Not yet implemented")
    }
}