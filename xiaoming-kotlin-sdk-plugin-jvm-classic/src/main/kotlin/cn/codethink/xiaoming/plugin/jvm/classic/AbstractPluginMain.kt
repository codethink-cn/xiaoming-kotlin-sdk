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
import cn.codethink.xiaoming.plugin.Plugin
import cn.codethink.xiaoming.plugin.PluginDisableContext
import cn.codethink.xiaoming.plugin.PluginEnableContext
import cn.codethink.xiaoming.plugin.PluginLoadContext
import cn.codethink.xiaoming.plugin.PluginUnloadContext
import cn.codethink.xiaoming.plugin.jvm.classic.util.orThrowException
import java.io.File

abstract class AbstractPluginMain : PluginMain {
    private var mutablePlugin: Plugin? = null
    override val plugin: Plugin get() = mutablePlugin.orThrowException()

    private var mutablePlatform: Platform? = null
    override val platform: Platform get() = mutablePlatform.orThrowException()

    private var mutableClassPath: JvmClassicPluginClassPath? = null
    override val classPath: JvmClassicPluginClassPath get() = mutableClassPath.orThrowException()

    private var mutableDirectoryFile: File? = null
    override val directoryFile: File get() = mutableDirectoryFile.orThrowException()

    internal fun onAllocate(
        plugin: Plugin,
        platform: Platform,
        classPath: JvmClassicPluginClassPath,
        directoryFile: File
    ) {
        this.mutablePlugin = plugin
        this.mutablePlatform = platform
        this.mutableClassPath = classPath
        this.mutableDirectoryFile = directoryFile

        onAllocate0()
    }

    abstract fun onAllocate0()

    internal fun onExit() {
        onExit0()

        this.mutablePlugin = null
        this.mutablePlatform = null
        this.mutableClassPath = null
        this.mutableDirectoryFile = null
    }

    abstract fun onExit0()

    @Throws(Exception::class)
    override suspend fun onLoad(context: PluginLoadContext) = Unit

    @Throws(Exception::class)
    override suspend fun onEnable(context: PluginEnableContext) = Unit

    @Throws(Exception::class)
    override suspend fun onDisable(context: PluginDisableContext) = Unit

    @Throws(Exception::class)
    override suspend fun onUnload(context: PluginUnloadContext) = Unit
}