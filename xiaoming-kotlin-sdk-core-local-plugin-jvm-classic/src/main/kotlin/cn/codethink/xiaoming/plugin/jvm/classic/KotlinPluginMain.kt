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
import cn.codethink.xiaoming.common.launchBy
import cn.codethink.xiaoming.plugin.id
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class KotlinClassicPluginContext(
    override val plugin: ClassicPlugin,
    override val cause: Cause,
    parentJob: Job,
    parentCoroutineContext: CoroutineContext,
    override val error: Cause? = null
) : ClassicPluginContext, CoroutineScope {
    private val supervisorJob = SupervisorJob(parentJob)
    private val scope = CoroutineScope(supervisorJob + parentCoroutineContext)
    override val coroutineContext: CoroutineContext by scope::coroutineContext

    val logger = KotlinLogging.logger { plugin.meta.logger ?: plugin.id.toString() }
}

interface KotlinPluginMain {
    /**
     * Do some initialization work when the plugin is loading.
     *
     * Loading sort is randomly (and maybe concurrently), do not rely on it. Make sure just
     * do operations like resources initialization and required environment checking.
     *
     * If unexpected error occurred, throw an exception to prevent the plugin from loading,
     * or set [KotlinClassicPluginContext.error] to a non-null value to mark the plugin as
     * errored. If it's null and no exception thrown, the plugin is considered as loaded
     * successfully.
     *
     * The `context` is a [CoroutineScope], you can use it to launch coroutines. It's highly
     * recommended to use [launchBy] to launch coroutines, otherwise you MUST provide cause
     * when calling platform provided APIs.
     *
     * @param context the context of this plugin.
     */
    fun onLoad(context: KotlinClassicPluginContext) = Unit
    fun onEnable(context: KotlinClassicPluginContext) = Unit
    fun onDisable(context: KotlinClassicPluginContext) = Unit
}

class KotlinPluginMainInvoker(
    val main: KotlinPluginMain
) : PluginMainInvoker {
    override fun invokeOnLoad(plugin: ClassicPlugin, cause: Cause) {
        TODO("Not yet implemented")
    }

    override fun invokeOnEnable(plugin: ClassicPlugin, cause: Cause) {
        TODO("Not yet implemented")
    }

    override fun invokeOnDisable(plugin: ClassicPlugin, cause: Cause) {
        TODO("Not yet implemented")
    }
}