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

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.event.EventContext
import cn.codethink.xiaoming.plugin.AvailablePlugin
import cn.codethink.xiaoming.plugin.PluginAllocateContext
import cn.codethink.xiaoming.plugin.PluginContext
import cn.codethink.xiaoming.plugin.PluginDisableContext
import cn.codethink.xiaoming.plugin.PluginDisableEvent
import cn.codethink.xiaoming.plugin.PluginEnableContext
import cn.codethink.xiaoming.plugin.PluginEnableEvent
import cn.codethink.xiaoming.plugin.PluginExitContext
import cn.codethink.xiaoming.plugin.PluginHandler
import cn.codethink.xiaoming.plugin.PluginLoadContext
import cn.codethink.xiaoming.plugin.PluginLoadEvent
import cn.codethink.xiaoming.plugin.PluginLogger
import cn.codethink.xiaoming.plugin.PluginUnloadContext
import cn.codethink.xiaoming.plugin.PluginUnloadEvent
import cn.codethink.xiaoming.plugin.id
import cn.codethink.xiaoming.plugin.jvm.classic.util.orThrowException
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.getOrConstruct
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.io.File
import kotlin.coroutines.CoroutineContext

@Suppress("UNCHECKED_CAST")
object KotlinPluginHandlerFactory : PluginHandlerFactory {
    private class KotlinPluginHandler(
        private val main: KotlinPluginMain,
        private val handler: JvmClassicPluginHandler
    ) : PluginHandler {
        private var mutableJob: Job? = null
        private val job: Job get() = mutableJob.orThrowException()

        private var mutableCoroutineScope: CoroutineScope? = null
        private val coroutineScope: CoroutineScope get() = mutableCoroutineScope.orThrowException()

        private var mutableLogger: KLogger? = null
        private val logger get() = mutableLogger.orThrowException()

        private abstract inner class AbstractKotlinPluginMainContext(
            private val context: PluginContext
        ) : KotlinPluginMainContext {
            override val classPath: JvmClassicPluginClassPath = handler.classPath
            override val logger: KLogger = this@KotlinPluginHandler.logger
            override val directoryFile: File = handler.directoryFile
            override val platform: Platform = context.platform
            override val plugin: AvailablePlugin = context.plugin as AvailablePlugin

            override suspend fun crash(operation: Operation) {
                context.crash(operation)
            }

            override suspend fun ensureCrashed(operation: Operation) {
                context.ensureCrashed(operation)
            }
        }

        override suspend fun onAllocate(context: PluginAllocateContext) {
            mutableLogger = PluginLogger(context.plugin.id, context.plugin as AvailablePlugin)
        }

        private inner class KotlinPluginMainLoadContextImpl(
            context: PluginLoadContext
        ) : AbstractKotlinPluginMainContext(context), KotlinPluginMainLoadContext {
            override val event: EventContext<PluginLoadEvent> = context.event
        }

        override suspend fun onLoad(context: PluginLoadContext) {
            main.onLoad(KotlinPluginMainLoadContextImpl(context))
        }

        private inner class KotlinPluginMainEnableContextImpl(
            context: PluginEnableContext
        ) : AbstractKotlinPluginMainContext(context), KotlinPluginMainEnableContext {
            override val coroutineContext: CoroutineContext get() = coroutineScope.coroutineContext
            override val event: EventContext<PluginEnableEvent> = context.event
        }

        override suspend fun onEnable(context: PluginEnableContext) {
            mutableJob = SupervisorJob()
            mutableCoroutineScope = CoroutineScope(job + context.platform.coroutineContext)

            main.onEnable(KotlinPluginMainEnableContextImpl(context))
        }

        private inner class KotlinPluginMainDisableContextImpl(
            context: PluginDisableContext
        ) : AbstractKotlinPluginMainContext(context), KotlinPluginMainDisableContext {
            override val coroutineContext: CoroutineContext get() = coroutineScope.coroutineContext
            override val event: EventContext<PluginDisableEvent> = context.event
        }

        override suspend fun onDisable(context: PluginDisableContext) {
            main.onDisable(KotlinPluginMainDisableContextImpl(context))

            job.cancel()
            mutableJob = null
            mutableCoroutineScope = null
        }

        private inner class KotlinPluginMainUnloadContextImpl(
            context: PluginUnloadContext
        ) : AbstractKotlinPluginMainContext(context), KotlinPluginMainUnloadContext {
            override val event: EventContext<PluginUnloadEvent> = context.event
        }

        override suspend fun onUnload(context: PluginUnloadContext) {
            main.onUnload(KotlinPluginMainUnloadContextImpl(context))
        }

        override suspend fun onExit(context: PluginExitContext) {}
    }

    override fun createPluginHandler(mainClass: Class<*>, handler: JvmClassicPluginHandler): PluginHandler {
        requireNotNull(KotlinPluginMain::class.java.isAssignableFrom(mainClass)) {
            "Plugin main class ${mainClass.name} is not a subclass of ${KotlinPluginMain::class.java.name}"
        }

        val main = getOrConstruct(mainClass as Class<out KotlinPluginMain>)
        return KotlinPluginHandler(main, handler)
    }
}