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

import cn.codethink.xiaoming.plugin.PluginAllocateContext
import cn.codethink.xiaoming.plugin.PluginDisableContext
import cn.codethink.xiaoming.plugin.PluginEnableContext
import cn.codethink.xiaoming.plugin.PluginExitContext
import cn.codethink.xiaoming.plugin.PluginHandler
import cn.codethink.xiaoming.plugin.PluginLoadContext
import cn.codethink.xiaoming.plugin.PluginUnloadContext
import cn.codethink.xiaoming.plugin.jvm.JvmPluginHandler
import cn.codethink.xiaoming.util.getOrConstruct
import java.io.File
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation

/**
 * 本地 JVM 经典插件：一个 [JvmClassicPluginHandler] 的非抽象子类，成为**插件主类**。
 * 为了方便，包含一个插件主类的 JAR 也可以被称为本地 JVM 经典插件，下称插件。
 *
 * 这种插件必须存在一个 `META-INF/xiaoming/plugin.yml` 资源文件，并在其内声明插件元数据信息。
 * 元数据包含 `main` 字段，其值为插件主类名。运行时，框架读取此文件，并从中获悉插件主类名，
 * 随后使用 [JvmClassicPluginClassPath.pluginClassLoader] 加载此类。
 *
 * 在同一个文件夹下，还可以存在 `access.yml` 文件，以声明插件类的访问权限。
 *
 * 尽管插件代码可能共享，但是插件在不同宿主上必须使用不同的 [JvmPluginHandler]。
 *
 * @author Chuanwise
 */
interface JvmClassicPluginHandler : JvmPluginHandler {
    /**
     * 插件类路径。
     */
    override val classPath: JvmClassicPluginClassPath

    /**
     * 插件目录。
     */
    val directoryFile: File
}

internal class JvmClassicPluginHandlerImpl(
    private val meta: JvmClassicPluginMeta,
    override val directoryFile: File,
    override val classPath: JvmClassicPluginClassPath
) : JvmClassicPluginHandler {
    private var mutableHandler: PluginHandler? = null
    private val handler: PluginHandler get() = mutableHandler ?: error("Plugin handler is not allocated")

    override suspend fun onAllocate(context: PluginAllocateContext) {
        require(mutableHandler == null) { "Plugin handler is already allocated" }

        // TODO: 满足插件的依赖库之类的需求
        classPath.repositories

        val mainClass = classPath.pluginClassLoader.loadClass(meta.main)
        val mainAnnotation = mainClass.kotlin.allSuperclasses.firstNotNullOfOrNull { it.findAnnotation<PluginMain>() }
        requireNotNull(mainAnnotation) { "Plugin main class ${meta.main} and all its super classes is not annotated with @PluginMain" }

        val handler = getOrConstruct(mainAnnotation.handlerFactory.java).createPluginHandler(mainClass, this)
        handler.onAllocate(context)

        mutableHandler = handler
    }

    override suspend fun onLoad(context: PluginLoadContext) {
        handler.onLoad(context)
    }

    override suspend fun onEnable(context: PluginEnableContext) {
        handler.onEnable(context)
    }

    override suspend fun onDisable(context: PluginDisableContext) {
        handler.onDisable(context)
    }

    override suspend fun onUnload(context: PluginUnloadContext) {
        handler.onUnload(context)
    }

    override suspend fun onExit(context: PluginExitContext) {
        handler.onExit(context)
    }
}