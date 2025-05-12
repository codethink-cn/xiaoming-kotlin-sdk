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
import cn.codethink.xiaoming.plugin.PluginLoadContext
import cn.codethink.xiaoming.plugin.PluginUnloadContext
import cn.codethink.xiaoming.plugin.jvm.JvmPluginHandler
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.getOrConstruct
import java.io.File

/**
 * 本地 JVM 经典插件：一个 [JvmClassicPluginHandler] 的非抽象子类，成为**插件主类**。
 * 为了方便，包含一个插件主类的 JAR 也可以被称为本地 JVM 经典插件，下称插件。
 *
 * 这种插件必须存在一个 `META-INF/xiaoming/plugin.yml` 资源文件，并在其内声明插件元数据信息。
 * 元数据包含 `main` 字段，其值为插件主类名。运行时，框架读取此文件，并从中获悉插件主类名，
 * 随后使用 [JvmClassicPluginClassPath.classLoader] 加载此类。
 *
 * 在同一个文件夹下，还可以存在 `access.yml` 文件，以声明插件类的访问权限。
 *
 * @author Chuanwise
 */
@OptIn(InternalApi::class)
class JvmClassicPluginHandler(
    private val meta: JvmClassicPluginMeta,
    private val directoryFile: File,
    override val classPath: JvmClassicPluginClassPath
) : JvmPluginHandler {
    private lateinit var main: AbstractPluginMain

    override suspend fun onAllocate(context: PluginAllocateContext) {
        val mainClass = classPath.classLoader.loadClass(meta.main)

        require(AbstractPluginMain::class.java.isAssignableFrom(mainClass)) {
            "Plugin main class ${meta.main} is not a subclass of ${AbstractPluginMain::class}"
        }
        main = getOrConstruct(mainClass) as AbstractPluginMain

        main.onAllocate(context.plugin, context.platform, classPath, directoryFile)
    }

    override suspend fun onLoad(context: PluginLoadContext) {
        main.onLoad(context)
    }

    override suspend fun onEnable(context: PluginEnableContext) {
        main.onEnable(context)
    }

    override suspend fun onDisable(context: PluginDisableContext) {
        main.onDisable(context)
    }

    override suspend fun onUnload(context: PluginUnloadContext) {
        main.onUnload(context)
    }

    override suspend fun onExit(context: PluginExitContext) {
        main.onExit()
    }
}